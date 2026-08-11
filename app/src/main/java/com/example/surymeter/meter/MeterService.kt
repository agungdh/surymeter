package com.example.surymeter.meter

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import com.example.surymeter.data.DailyUsage
import com.example.surymeter.data.DayKey
import com.example.surymeter.data.PersistedState
import com.example.surymeter.data.PrevState
import com.example.surymeter.data.Settings
import com.example.surymeter.data.SignalReader
import com.example.surymeter.data.TrafficReader
import com.example.surymeter.data.TrafficSnapshot
import com.example.surymeter.data.UsageStorage
import com.example.surymeter.data.UsageTracker
import com.example.surymeter.data.toTotals
import com.example.surymeter.ui.Format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MeterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var storage: UsageStorage
    private var job: Job? = null

    private var state: PersistedState? = null
    private var prev: PrevState? = null
    private var day: DailyUsage? = null
    private val overlay = SpeedOverlay()
    private val screenReceiver = ScreenReceiver()

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        MeterNotification.createChannel(this)
        storage = UsageStorage(this)
        registerScreenReceiver()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        try {
            registerReceiver(screenReceiver, filter)
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == MeterNotification.ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job == null) {
            val loaded = storage.load()
            state = loaded
            startForeground(
                MeterNotification.NOTIFICATION_ID,
                MeterNotification.build(
                    this,
                    MeterUiState().speeds,
                    today = loaded.totals,
                    totals = loaded.totals
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
            MeterState.update {
                it.copy(
                    running = true,
                    totals = loaded.totals,
                    today = loaded.daily[DayKey.today()] ?: DailyUsage.empty(DayKey.today()),
                    days = loaded.daily.values.sortedByDescending { d -> d.date }
                )
            }
            if (AndroidSettings.canDrawOverlays(this)) {
                overlay.show(this)
            }
            job = scope.launch { meterLoop(loaded) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        finalFlush()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        scope.cancel()
        job = null
        state = null
        prev = null
        day = null
        overlay.hide(this)
        MeterState.update { it.copy(running = false) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun CoroutineScope.meterLoop(loaded: PersistedState) {
        var prevState = PrevState(
            lastTotalRx = loaded.lastTotalRx,
            lastTotalTx = loaded.lastTotalTx,
            lastWifiRx = loaded.lastWifiRx,
            lastWifiTx = loaded.lastWifiTx,
            lastMobileRx = loaded.lastMobileRx,
            lastMobileTx = loaded.lastMobileTx,
            timestamp = SystemClock.elapsedRealtime(),
            totals = loaded.totals
        )
        var todayKey = DayKey.today()
        var today = loaded.daily[todayKey] ?: DailyUsage.empty(todayKey)
        var lastFlush = SystemClock.elapsedRealtime()
        val notifier = getSystemService(NotificationManager::class.java)

        MeterState.update {
            it.copy(
                running = true,
                totals = prevState.totals,
                today = today,
                days = loaded.daily.values.sortedByDescending { d -> d.date }
            )
        }

        while (isActive) {
            val nowTime = SystemClock.elapsedRealtime()
            val screenOnNow = MeterState.snapshot().screenOn
            val pausedForScreenOff = Settings.pauseWhenScreenOff && !screenOnNow

            if (!pausedForScreenOff) {
                val snapshot = readSnapshot()
                val result = UsageTracker.sample(prevState, snapshot, nowTime)
                prevState = result.newLast
                prev = prevState
                state = loaded
                day = today

                val currentKey = DayKey.today()
                if (currentKey != todayKey) {
                    loaded.daily[today.date] = today
                    today = DailyUsage.empty(currentKey)
                    todayKey = currentKey
                }
                today = DailyUsage(
                    date = today.date,
                    wifiRx = today.wifiRx + result.delta.wifiRx,
                    wifiTx = today.wifiTx + result.delta.wifiTx,
                    mobileRx = today.mobileRx + result.delta.mobileRx,
                    mobileTx = today.mobileTx + result.delta.mobileTx
                )
                day = today

                MeterState.update {
                    it.copy(
                        speeds = result.speeds,
                        totals = result.newTotals,
                        today = today,
                        screenOn = screenOnNow
                    )
                }

                val (speedNum, speedUnit) = Format.speedParts(
                    maxOf(result.speeds.wifiRx, result.speeds.mobileRx),
                    Settings.useBits
                )
                overlay.update(speedNum, speedUnit)

                if (nowTime - lastFlush >= FLUSH_INTERVAL_MS) {
                    flush(loaded, prevState, today)
                    lastFlush = nowTime
                }
            }

            MeterState.update { it.copy(screenOn = screenOnNow) }
            notifier.notify(
                MeterNotification.NOTIFICATION_ID,
                MeterNotification.build(
                    this@MeterService,
                    MeterState.snapshot().speeds,
                    SignalReader.read(this@MeterService),
                    today.toTotals(),
                    prevState.totals
                )
            )

            delay(if (pausedForScreenOff) SAMPLE_INTERVAL_SCREEN_OFF_MS else SAMPLE_INTERVAL_MS)
        }
    }

    private fun flush(state: PersistedState, prev: PrevState, day: DailyUsage) {
        state.daily[day.date] = day
        state.totals = prev.totals
        state.lastTotalRx = prev.lastTotalRx
        state.lastTotalTx = prev.lastTotalTx
        state.lastWifiRx = prev.lastWifiRx
        state.lastWifiTx = prev.lastWifiTx
        state.lastMobileRx = prev.lastMobileRx
        state.lastMobileTx = prev.lastMobileTx
        storage.save(state)
        MeterState.update {
            it.copy(days = state.daily.values.sortedByDescending { d -> d.date })
        }
    }

    private fun finalFlush() {
        val s = state ?: return
        val p = prev ?: return
        val d = day ?: return
        flush(s, p, d)
    }

    private fun readSnapshot(): TrafficSnapshot = try {
        TrafficReader.snapshot()
    } catch (_: Exception) {
        TrafficSnapshot(0, 0, 0, 0, 0, 0)
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 1000L
        private const val SAMPLE_INTERVAL_SCREEN_OFF_MS = 60_000L
        private const val FLUSH_INTERVAL_MS = 30_000L
    }
}
