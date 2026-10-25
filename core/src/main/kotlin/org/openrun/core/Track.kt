package org.openrun.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPoint(val lat: Double, val lon: Double, val accuracyM: Float, val timeMs: Long)

object Track {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val LAP_M = 400.0       // one lap of an athletics track
    private const val MAX_SPEED_MPS = 12.0 // faster than that between two fixes is a GPS jump, not a runner

    fun haversineM(a: TrackPoint, b: TrackPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    /** Distance of a recorded track, ignoring imprecise fixes and impossible jumps. */
    fun distanceM(points: List<TrackPoint>, maxAccuracyM: Float = 30f): Double {
        var total = 0.0
        var last: TrackPoint? = null
        for (p in points) {
            if (p.accuracyM > maxAccuracyM) continue
            val l = last
            if (l != null) {
                val d = haversineM(l, p)
                val dt = (p.timeMs - l.timeMs) / 1000.0
                if (dt > 0 && d / dt <= MAX_SPEED_MPS) total += d else if (dt > 0) continue
            }
            last = p
        }
        return total
    }

    /** Seconds per kilometre, or null when there is nothing to divide by yet. */
    fun paceSecPerKm(distanceM: Double, seconds: Double): Double? =
        if (distanceM < 10 || seconds <= 0) null else seconds / (distanceM / 1000)

    fun laps(distanceM: Double): Double = distanceM / LAP_M
}
