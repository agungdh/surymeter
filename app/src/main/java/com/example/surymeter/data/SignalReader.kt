package com.example.surymeter.data

import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager

data class SignalInfo(
    val wifiPct: Int? = null,
    val wifiSsid: String? = null,
    val mobilePct: Int? = null
)

object SignalReader {

    fun read(context: Context): SignalInfo {
        return SignalInfo(
            wifiPct = readWifiRssi(context),
            wifiSsid = readWifiSsid(context),
            mobilePct = readMobileSignal(context)
        )
    }

    private fun readWifiRssi(context: Context): Int? {
        return try {
            val wm = context.getSystemService(WifiManager::class.java) ?: return null
            val info = wm.connectionInfo ?: return null
            val rssi = info.rssi
            if (rssi in -100..-20) {
                ((rssi + 100) * 100 / 45).coerceIn(0, 100)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readWifiSsid(context: Context): String? {
        return try {
            val wm = context.getSystemService(WifiManager::class.java) ?: return null
            val ssid = wm.connectionInfo?.ssid
            ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        } catch (_: Exception) {
            null
        }
    }

    private fun readMobileSignal(context: Context): Int? {
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return null
            val strength = tm.signalStrength ?: return null
            val level = strength.level
            if (level in 0..4) level * 25 else null
        } catch (_: Exception) {
            null
        }
    }
}
