package com.example.surymeter.data

import android.content.Context
import org.json.JSONObject

class PersistedState(
    var totals: Totals = Totals(),
    var lastTotalRx: Long = -1,
    var lastTotalTx: Long = -1,
    var lastMobileRx: Long = -1,
    var lastMobileTx: Long = -1,
    val daily: MutableMap<String, DailyUsage> = LinkedHashMap()
) {
    fun currentDay(): DailyUsage? = daily[DayKey.today()]
}

object DayKey {
    private val formatter: java.time.format.DateTimeFormatter =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun today(): String = java.time.LocalDate.now().format(formatter)
    fun key(date: java.time.LocalDate): String = date.format(formatter)
    fun parse(key: String): java.time.LocalDate =
        java.time.LocalDate.parse(key, formatter)
}

class UsageStorage(context: Context) {

    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)

    fun load(): PersistedState {
        val state = PersistedState(
            totals = Totals(
                wifiRx = prefs.getLong("acc_wifi_rx", 0),
                wifiTx = prefs.getLong("acc_wifi_tx", 0),
                mobileRx = prefs.getLong("acc_mobile_rx", 0),
                mobileTx = prefs.getLong("acc_mobile_tx", 0)
            ),
            lastTotalRx = prefs.getLong("last_total_rx", -1),
            lastTotalTx = prefs.getLong("last_total_tx", -1),
            lastMobileRx = prefs.getLong("last_mobile_rx", -1),
            lastMobileTx = prefs.getLong("last_mobile_tx", -1)
        )
        val json = prefs.getString("daily", null)
        if (json != null) {
            try {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val d = obj.optJSONObject(key) ?: continue
                    state.daily[key] = DailyUsage(
                        date = key,
                        wifiRx = d.optLong("wifiRx"),
                        wifiTx = d.optLong("wifiTx"),
                        mobileRx = d.optLong("mobileRx"),
                        mobileTx = d.optLong("mobileTx")
                    )
                }
            } catch (_: Exception) {
            }
        }
        return state
    }

    fun save(state: PersistedState) {
        val editor = prefs.edit()
        editor.putLong("acc_wifi_rx", state.totals.wifiRx)
        editor.putLong("acc_wifi_tx", state.totals.wifiTx)
        editor.putLong("acc_mobile_rx", state.totals.mobileRx)
        editor.putLong("acc_mobile_tx", state.totals.mobileTx)
        editor.putLong("last_total_rx", state.lastTotalRx)
        editor.putLong("last_total_tx", state.lastTotalTx)
        editor.putLong("last_mobile_rx", state.lastMobileRx)
        editor.putLong("last_mobile_tx", state.lastMobileTx)

        val dailyObj = JSONObject()
        for ((key, d) in state.daily) {
            dailyObj.put(
                key,
                JSONObject()
                    .put("wifiRx", d.wifiRx)
                    .put("wifiTx", d.wifiTx)
                    .put("mobileRx", d.mobileRx)
                    .put("mobileTx", d.mobileTx)
            )
        }
        editor.putString("daily", dailyObj.toString())
        editor.apply()
    }
}
