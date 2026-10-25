package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackShapeTest {
    private fun p(lat: Double, lon: Double, acc: Float = 5f) = TrackPoint(lat, lon, acc, 0)

    @Test fun nothingToDrawFromLessThanTwoPoints() {
        assertNull(TrackShape.project(emptyList()))
        assertNull(TrackShape.project(listOf(p(48.0, 2.0))))
        assertNull(TrackShape.project(listOf(p(48.0, 2.0), p(48.0, 2.0)))) // a runner who never moved
    }

    @Test fun everyPointStaysInsideTheUnitSquare() {
        val t = TrackShape.project(listOf(p(48.000, 2.000), p(48.001, 2.002), p(48.002, 2.001), p(48.0005, 2.0035)))!!
        assertTrue(t.points.all { it.first in 0f..1f && it.second in 0f..1f })
    }

    @Test fun northIsUp() {
        val t = TrackShape.project(listOf(p(48.000, 2.0), p(48.002, 2.0)))!!
        val south = t.points[0].second
        val north = t.points[1].second
        assertTrue("north has the smaller y", north < south)
    }

    @Test fun aLongNorthSouthRouteIsTallAndNarrow() {
        val t = TrackShape.project(listOf(p(48.000, 2.0), p(48.010, 2.0), p(48.010, 2.001)))!!
        assertEquals(1f, t.heightFraction, 0.0001f)
        assertTrue(t.widthFraction < 0.2f)
    }

    @Test fun degreesOfLongitudeAreShorterThanDegreesOfLatitude() {
        // at 48 degrees north, 1 degree of longitude is about 0.67 of a degree of latitude
        val t = TrackShape.project(listOf(p(48.0, 2.0), p(48.001, 2.001)))!!
        assertEquals(0.67f, t.widthFraction / t.heightFraction, 0.02f)
    }

    @Test fun impreciseFixesAreLeftOut() {
        val t = TrackShape.project(listOf(p(48.0, 2.0), p(48.5, 2.5, acc = 200f), p(48.001, 2.0)))!!
        assertEquals(2, t.points.size)
    }
}
