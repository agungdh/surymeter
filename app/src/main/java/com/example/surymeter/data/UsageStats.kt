package com.example.surymeter.data

data class TrafficSnapshot(
    val totalRx: Long,
    val totalTx: Long,
    val mobileRx: Long,
    val mobileTx: Long
)

data class Speeds(
    val wifiRx: Long = 0,
    val wifiTx: Long = 0,
    val mobileRx: Long = 0,
    val mobileTx: Long = 0
)

data class DailyUsage(
    val date: String,
    val wifiRx: Long,
    val wifiTx: Long,
    val mobileRx: Long,
    val mobileTx: Long
) {
    val totalRx: Long get() = wifiRx + mobileRx
    val totalTx: Long get() = wifiTx + mobileTx
    val total: Long get() = totalRx + totalTx

    companion object {
        fun empty(date: String = "") = DailyUsage(date, 0, 0, 0, 0)
    }
}

data class Totals(
    val wifiRx: Long = 0,
    val wifiTx: Long = 0,
    val mobileRx: Long = 0,
    val mobileTx: Long = 0
) {
    val totalRx: Long get() = wifiRx + mobileRx
    val totalTx: Long get() = wifiTx + mobileTx
    val total: Long get() = totalRx + totalTx
    val wifiTotal: Long get() = wifiRx + wifiTx
    val mobileTotal: Long get() = mobileRx + mobileTx
}

fun TrafficSnapshot.wifiRx(): Long = totalRx - mobileRx
fun TrafficSnapshot.wifiTx(): Long = totalTx - mobileTx

fun DailyUsage.toTotals(): Totals = Totals(wifiRx, wifiTx, mobileRx, mobileTx)
