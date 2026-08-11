package com.example.surymeter

import com.example.surymeter.data.PrevState
import com.example.surymeter.data.Speeds
import com.example.surymeter.data.Totals
import com.example.surymeter.data.TrafficSnapshot
import com.example.surymeter.data.UsageTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageTrackerTest {

    private fun snapshot(
        totalRx: Long, totalTx: Long,
        wifiRx: Long, wifiTx: Long,
        mobileRx: Long, mobileTx: Long
    ) = TrafficSnapshot(totalRx, totalTx, wifiRx, wifiTx, mobileRx, mobileTx)

    private fun prev() = PrevState(
        lastTotalRx = 1000,
        lastTotalTx = 500,
        lastWifiRx = 600,
        lastWifiTx = 200,
        lastMobileRx = 400,
        lastMobileTx = 300,
        timestamp = 0,
        totals = Totals()
    )

    @Test
    fun wifiDeltaComputedDirectlyFromWifiCounters() {
        val now = snapshot(
            totalRx = 2000, totalTx = 1000,
            wifiRx = 700, wifiTx = 250,
            mobileRx = 400, mobileTx = 300
        )
        val result = UsageTracker.sample(prev(), now, 1000)

        assertEquals(100, result.delta.wifiRx)
        assertEquals(50, result.delta.wifiTx)
        assertEquals(0, result.delta.mobileRx)
        assertEquals(0, result.delta.mobileTx)
        // wifi is NOT derived from total-mobile
        assertEquals(100, result.speeds.wifiRx)
    }

    @Test
    fun speedsUseDeltaOverDt() {
        val now = snapshot(
            totalRx = 2000, totalTx = 1000,
            wifiRx = 700, wifiTx = 250,
            mobileRx = 800, mobileTx = 500
        )
        val result = UsageTracker.sample(prev(), now, 2000)

        // dt = 2s -> wifi rx 100/2 = 50 B/s, mobile rx 400/2 = 200 B/s
        assertEquals(50, result.speeds.wifiRx)
        assertEquals(200, result.speeds.mobileRx)
    }

    @Test
    fun countersWrappedAreIgnored() {
        val now = snapshot(
            totalRx = 2000, totalTx = 1000,
            wifiRx = 100, wifiTx = 50,
            mobileRx = 400, mobileTx = 300
        )
        val result = UsageTracker.sample(prev(), now, 1000)

        assertEquals(0, result.delta.wifiRx)
        assertEquals(0, result.delta.wifiTx)
        assertEquals(0, result.delta.mobileRx)
    }

    @Test
    fun newLastTracksAllCounters() {
        val now = snapshot(
            totalRx = 2000, totalTx = 1000,
            wifiRx = 700, wifiTx = 250,
            mobileRx = 800, mobileTx = 500
        )
        val result = UsageTracker.sample(prev(), now, 1000)

        assertEquals(700, result.newLast.lastWifiRx)
        assertEquals(250, result.newLast.lastWifiTx)
        assertEquals(800, result.newLast.lastMobileRx)
        assertEquals(500, result.newLast.lastMobileTx)
        assertTrue(result.newLast.totals.wifiRx > 0)
    }

    @Test
    fun totalsAccumulateAcrossSamples() {
        val p = prev()
        val s1 = UsageTracker.sample(
            p,
            snapshot(2000, 1000, 700, 250, 800, 500),
            1000
        )
        val s2 = UsageTracker.sample(
            s1.newLast,
            snapshot(3000, 1500, 900, 300, 1200, 600),
            2000
        )

        assertEquals(300, s2.newTotals.wifiRx)
        assertEquals(100, s2.newTotals.wifiTx)
        assertEquals(800, s2.newTotals.mobileRx)
        assertEquals(300, s2.newTotals.mobileTx)
    }
}
