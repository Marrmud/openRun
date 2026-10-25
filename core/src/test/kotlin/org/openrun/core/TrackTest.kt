package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TrackTest {
    // 0.001 degree of latitude is about 111.2 m anywhere
    private fun p(latDeg: Double, tSec: Long, acc: Float = 5f) = TrackPoint(latDeg, 2.0, acc, tSec * 1000)

    @Test fun twoPointsAreAboutOneHundredElevenMetresApart() {
        assertEquals(111.2, Track.haversineM(p(48.000, 0), p(48.001, 1)), 0.5)
    }

    @Test fun distanceAddsTheLegs() {
        val pts = listOf(p(48.000, 0), p(48.001, 40), p(48.002, 80))
        assertEquals(222.4, Track.distanceM(pts), 1.0)
    }

    @Test fun impreciseFixesAreIgnored() {
        val pts = listOf(p(48.000, 0), p(48.0005, 20, acc = 80f), p(48.001, 40))
        assertEquals(111.2, Track.distanceM(pts), 1.0)
    }

    @Test fun aGpsJumpIsNotCounted() {
        // 1 km in one second is not a runner
        val pts = listOf(p(48.000, 0), p(48.009, 1), p(48.0001, 30))
        assertEquals(11.1, Track.distanceM(pts), 1.0)
    }

    @Test fun nothingToMeasureWithOnePoint() {
        assertEquals(0.0, Track.distanceM(listOf(p(48.0, 0))), 0.0)
        assertEquals(0.0, Track.distanceM(emptyList()), 0.0)
    }

    @Test fun pace() {
        assertEquals(360.0, Track.paceSecPerKm(5000.0, 1800.0)!!, 0.001) // 6:00 per km
        assertNull(Track.paceSecPerKm(0.0, 100.0))
        assertNull(Track.paceSecPerKm(1000.0, 0.0))
        assertNotNull(Track.paceSecPerKm(100.0, 30.0))
    }

    @Test fun nineLapsForThreePointSixKilometres() {
        assertEquals(9.0, Track.laps(3600.0), 0.0001)
    }
}
