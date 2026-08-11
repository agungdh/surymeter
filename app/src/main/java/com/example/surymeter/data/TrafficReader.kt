package com.example.surymeter.data

import android.net.TrafficStats
import java.io.File

/**
 * Reads per-interface traffic counters from /proc/net/dev so wifi and mobile
 * usage can be tracked independently (mimicking the native lib used by the
 * reference app, but without requiring a .so). Falls back to TrafficStats.
 */
object TrafficReader {

    fun snapshot(): TrafficSnapshot {
        val proc = readProcNetDev()
        if (proc != null) {
            val wifiRx = sumInterfaces(proc, ::isWifi)
            val wifiTx = sumInterfaces(proc, ::isWifi, tx = true)
            val mobileRx = sumInterfaces(proc, ::isMobile)
            val mobileTx = sumInterfaces(proc, ::isMobile, tx = true)
            val totalRx = sumAll(proc, tx = false)
            val totalTx = sumAll(proc, tx = true)
            if (totalRx > 0 || totalTx > 0) {
                return TrafficSnapshot(totalRx, totalTx, wifiRx, wifiTx, mobileRx, mobileTx)
            }
        }
        val totalRx2 = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
        val totalTx2 = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
        val mobileRx2 = TrafficStats.getMobileRxBytes().coerceAtLeast(0)
        val mobileTx2 = TrafficStats.getMobileTxBytes().coerceAtLeast(0)
        val wifiRx2 = (totalRx2 - mobileRx2).coerceAtLeast(0)
        val wifiTx2 = (totalTx2 - mobileTx2).coerceAtLeast(0)
        return TrafficSnapshot(totalRx2, totalTx2, wifiRx2, wifiTx2, mobileRx2, mobileTx2)
    }

    private class Iface(val name: String, val rx: Long, val tx: Long)

    private fun readProcNetDev(): List<Iface>? {
        return try {
            val file = File("/proc/net/dev")
            if (!file.exists()) return null
            file.readLines()
                .drop(2)
                .mapNotNull { line ->
                    val trimmed = line.trim()
                    val idx = trimmed.indexOf(':')
                    if (idx < 0) return@mapNotNull null
                    val name = trimmed.substring(0, idx).trim()
                    val values = trimmed.substring(idx + 1).trim().split(Regex("\\s+"))
                    if (values.size < 8) return@mapNotNull null
                    val rx = values[0].toLongOrNull() ?: 0L
                    val tx = values[8].toLongOrNull() ?: 0L
                    Iface(name, rx, tx)
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun sumInterfaces(ifaces: List<Iface>, match: (String) -> Boolean, tx: Boolean = false): Long {
        var sum = 0L
        for (i in ifaces) {
            if (match(i.name)) {
                sum += if (tx) i.tx else i.rx
            }
        }
        return sum
    }

    private fun sumAll(ifaces: List<Iface>, tx: Boolean = false): Long {
        var sum = 0L
        for (i in ifaces) {
            if (i.name == "lo") continue
            sum += if (tx) i.tx else i.rx
        }
        return sum
    }

    private fun isWifi(name: String): Boolean {
        return name.startsWith("wlan") ||
            name.startsWith("eth") ||
            name.startsWith("ap") ||
            name.startsWith("p2p") ||
            name.startsWith("wifi")
    }

    private fun isMobile(name: String): Boolean {
        return name.startsWith("rmnet") ||
            name.startsWith("ccmni") ||
            name.startsWith("rndis") ||
            name.startsWith("wwan") ||
            name.startsWith("pdp") ||
            name.startsWith("usb") ||
            name.startsWith("ppp")
    }
}
