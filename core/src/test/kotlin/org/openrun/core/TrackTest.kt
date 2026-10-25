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

    @Test fun theLiveAccumulatorAgreesWithAddingUpTheWholeTrack() {
        val pts = listOf(p(48.000, 0), p(48.001, 40), p(48.0015, 60, acc = 90f), p(48.009, 61), p(48.002, 80), p(48.0021, 81, acc = 5f))
        val acc = TrackAccumulator()
        var live = 0.0
        pts.forEach { live += acc.add(it) }
        assertEquals(Track.distanceM(pts), live, 0.0001)
        assertEquals(live, acc.distanceM, 0.0001)
    }

    @Test fun anIgnoredFixAddsNothing() {
        val acc = TrackAccumulator()
        acc.add(p(48.000, 0))
        assertEquals(0.0, acc.add(p(48.001, 40, acc = 80f)), 0.0)
        assertEquals(0.0, acc.add(p(48.009, 41)), 0.0) // 1 km in a second
        assertEquals(111.2, acc.add(p(48.001, 60)), 0.5)
    }

    @Test fun recentPaceLooksOnlyAtTheLastWindow() {
        // slow for a minute (1 m/s), then fast for 30 s (4 m/s): the pace right now is the fast one
        val pts = mutableListOf<TrackPoint>()
        var lat = 48.0
        for (t in 0..60) { pts += p(lat, t.toLong()); lat += 1.0 / 111_195.0 }
        for (t in 61..90) { pts += p(lat, t.toLong()); lat += 4.0 / 111_195.0 }
        val pace = Track.recentPaceSecPerKm(pts, windowMs = 30_000)!!
        assertEquals(250.0, pace, 12.0) // 4 m/s is 4:10 per km
    }

    @Test fun noRecentPaceWithoutEnoughPoints() {
        assertNull(Track.recentPaceSecPerKm(emptyList()))
        assertNull(Track.recentPaceSecPerKm(listOf(p(48.0, 0))))
    }

    @Test fun nineLapsForThreePointSixKilometres() {
        assertEquals(9.0, Track.laps(3600.0), 0.0001)
    }
}
