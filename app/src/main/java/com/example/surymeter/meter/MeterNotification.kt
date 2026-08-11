package com.example.surymeter.meter

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.app.NotificationCompat
import com.example.surymeter.MainActivity
import com.example.surymeter.R
import com.example.surymeter.data.NotifStyle
import com.example.surymeter.data.Settings
import com.example.surymeter.data.SignalInfo
import com.example.surymeter.data.Speeds
import com.example.surymeter.data.Totals
import com.example.surymeter.ui.Format

object MeterNotification {

    const val CHANNEL_ID = "meter_channel"
    const val NOTIFICATION_ID = 1
    const val ACTION_STOP = "com.example.surymeter.action.STOP"

    fun createChannel(context: Context) {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Internet usage",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows current bandwidth and usage"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        speeds: Speeds,
        signal: SignalInfo = SignalInfo(),
        today: Totals,
        totals: Totals
    ): Notification {
        val settings = Settings
        settings.init(context)
        val style = settings.notifStyle
        val useBits = settings.useBits
        val showSignal = settings.showSignal

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MeterService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = android.widget.RemoteViews(
            context.packageName,
            R.layout.notification_content
        )
        val mainDown = maxOf(speeds.wifiRx, speeds.mobileRx)
        val (speedNum, speedUnit) = Format.speedParts(mainDown, useBits)
        content.setTextViewText(R.id.title, context.getString(R.string.notif_title))
        content.setTextViewText(
            R.id.total_today,
            context.getString(
                R.string.notif_total_today,
                Format.bytes(today.total)
            )
        )
        content.setTextViewText(R.id.speed_value, speedNum)
        content.setTextViewText(R.id.speed_unit, speedUnit)
        content.setTextViewText(R.id.wifi_label, context.getString(R.string.net_wifi))
        content.setTextViewText(
            R.id.wifi_line,
            "↓ ${Format.speed(speeds.wifiRx)}  ↑ ${Format.speed(speeds.wifiTx)}"
        )
        content.setTextViewText(R.id.mobile_label, context.getString(R.string.net_mobile))
        content.setTextViewText(
            R.id.mobile_line,
            "↓ ${Format.speed(speeds.mobileRx)}  ↑ ${Format.speed(speeds.mobileTx)}"
        )
        content.setTextViewText(
            R.id.updown_line,
            "↓ ${Format.speed(mainDown)}  ↑ ${Format.speed(maxOf(speeds.wifiTx, speeds.mobileTx))}"
        )

        when (style) {
            NotifStyle.SPEED_ONLY -> {
                content.setViewVisibility(R.id.updown_block, View.GONE)
                content.setViewVisibility(R.id.network_block, View.GONE)
            }
            NotifStyle.UP_DOWN -> {
                content.setViewVisibility(R.id.updown_block, View.VISIBLE)
                content.setViewVisibility(R.id.network_block, View.GONE)
            }
            NotifStyle.NETWORKS -> {
                content.setViewVisibility(R.id.updown_block, View.GONE)
                content.setViewVisibility(R.id.network_block, View.VISIBLE)
            }
            NotifStyle.FULL -> {
                content.setViewVisibility(R.id.updown_block, View.VISIBLE)
                content.setViewVisibility(R.id.network_block, View.VISIBLE)
            }
        }

        if (showSignal) {
            content.setViewVisibility(R.id.signal_block, View.VISIBLE)
            content.setTextViewText(R.id.signal_line, formatSignal(signal))
        } else {
            content.setViewVisibility(R.id.signal_block, View.GONE)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(Format.bytes(totals.total))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openIntent)
            .setCustomContentView(content)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.notif_stop),
                    stopIntent
                ).build()
            )

        if (settings.showOnLockscreen) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        return builder.build()
    }

    private fun formatSignal(signal: SignalInfo): String {
        val wifi = signal.wifiPct?.let { "$it%" } ?: "-"
        val mobile = signal.mobilePct?.let { "$it%" } ?: "-"
        val ssid = signal.wifiSsid?.let { " ($it)" } ?: ""
        return "WiFi $wifi$ssid  ·  Data $mobile"
    }
}
