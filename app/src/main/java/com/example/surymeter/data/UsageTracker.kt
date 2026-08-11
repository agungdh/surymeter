package com.example.surymeter.data

data class PrevState(
    val lastTotalRx: Long,
    val lastTotalTx: Long,
    val lastWifiRx: Long,
    val lastWifiTx: Long,
    val lastMobileRx: Long,
    val lastMobileTx: Long,
    val timestamp: Long,
    val totals: Totals
)

data class SampleResult(
    val delta: Totals,
    val speeds: Speeds,
    val newTotals: Totals,
    val newLast: PrevState
)

object UsageTracker {

    fun sample(prev: PrevState?, now: TrafficSnapshot, nowTime: Long): SampleResult {
        val dtSeconds = if (prev == null) 0.0 else (nowTime - prev.timestamp).coerceAtLeast(0) / 1000.0

        val dTotalRx = delta(now.totalRx, prev?.lastTotalRx)
        val dTotalTx = delta(now.totalTx, prev?.lastTotalTx)
        val dWifiRx = delta(now.wifiRx, prev?.lastWifiRx)
        val dWifiTx = delta(now.wifiTx, prev?.lastWifiTx)
        val dMobileRx = delta(now.mobileRx, prev?.lastMobileRx)
        val dMobileTx = delta(now.mobileTx, prev?.lastMobileTx)

        val newTotals = Totals(
            wifiRx = (prev?.totals?.wifiRx ?: 0) + dWifiRx,
            wifiTx = (prev?.totals?.wifiTx ?: 0) + dWifiTx,
            mobileRx = (prev?.totals?.mobileRx ?: 0) + dMobileRx,
            mobileTx = (prev?.totals?.mobileTx ?: 0) + dMobileTx
        )

        fun speed(bytes: Long): Long =
            if (dtSeconds > 0) Math.round(bytes / dtSeconds) else 0

        val result = SampleResult(
            delta = Totals(dWifiRx, dWifiTx, dMobileRx, dMobileTx),
            speeds = Speeds(
                wifiRx = speed(dWifiRx),
                wifiTx = speed(dWifiTx),
                mobileRx = speed(dMobileRx),
                mobileTx = speed(dMobileTx)
            ),
            newTotals = newTotals,
            newLast = PrevState(
                lastTotalRx = now.totalRx,
                lastTotalTx = now.totalTx,
                lastWifiRx = now.wifiRx,
                lastWifiTx = now.wifiTx,
                lastMobileRx = now.mobileRx,
                lastMobileTx = now.mobileTx,
                timestamp = nowTime,
                totals = newTotals
            )
        )
        return result
    }

    private fun delta(now: Long, last: Long?): Long =
        if (last == null || last < 0 || now < last) 0 else now - last
}
