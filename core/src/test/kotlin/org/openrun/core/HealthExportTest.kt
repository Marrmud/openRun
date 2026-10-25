package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthExportTest {
    private fun p(t: Long, acc: Float = 5f) = TrackPoint(48.0 + t * 1e-6, 2.0, acc, t)

    @Test fun theSameSessionAlwaysGetsTheSameId() {
        assertEquals(HealthExport.clientRecordId(7), HealthExport.clientRecordId(7))
        assertNotEquals(HealthExport.clientRecordId(7), HealthExport.clientRecordId(8))
        assertNotEquals(HealthExport.clientRecordId(7, "session"), HealthExport.clientRecordId(7, "distance"))
    }

    @Test fun theWindowIsTheActiveDurationWhenNoFixRunsPastIt() {
        val (s, e) = HealthExport.window(1_000, 60, listOf(p(2_000), p(30_000)))
        assertEquals(1_000L, s); assertEquals(61_000L, e)
    }

    @Test fun theWindowStretchesToTheLastFixAfterAPause() {
        // 60 s of active time, but the last fix came 120 s after the start
        val (_, e) = HealthExport.window(1_000, 60, listOf(p(2_000), p(121_000)))
        assertEquals(121_000L, e)
    }

    @Test fun aFixBeforeTheStartDoesNotMoveTheEnd() {
        val (_, e) = HealthExport.window(50_000, 10, listOf(p(1_000)))
        assertEquals(60_000L, e)
    }

    @Test fun theWindowIsNeverEmpty() {
        val (s, e) = HealthExport.window(5_000, 0, emptyList())
        assertTrue(e > s)
    }

    @Test fun theRouteKeepsOnlyPreciseFixesInsideTheWindowInOrder() {
        val pts = listOf(p(9_000), p(2_000), p(3_000, acc = 120f), p(500), p(4_000), p(4_000), p(20_000))
        val route = HealthExport.routePoints(pts, 1_000, 10_000)
        assertEquals(listOf(2_000L, 4_000L, 9_000L), route.map { it.timeMs })
    }

    @Test fun noRouteWithFewerThanTwoPoints() {
        assertTrue(HealthExport.routePoints(listOf(p(2_000)), 1_000, 10_000).isEmpty())
        assertTrue(HealthExport.routePoints(emptyList(), 1_000, 10_000).isEmpty())
    }
}
