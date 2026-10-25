package org.openrun.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPoint(val lat: Double, val lon: Double, val accuracyM: Float, val timeMs: Long)

/**
 * Adds GPS fixes one at a time and keeps the distance. Imprecise fixes and impossible jumps are ignored, so the
 * same figure comes out live during a session and when the whole track is added up afterwards.
 */
class TrackAccumulator(private val maxAccuracyM: Float = 30f) {
    var distanceM: Double = 0.0
        private set
    private var last: TrackPoint? = null

    /** Distance added by this fix (0 when it is ignored or is the first). */
    fun add(p: TrackPoint): Double {
        if (p.accuracyM > maxAccuracyM) return 0.0
        val l = last
        var added = 0.0
        if (l != null) {
            val d = Track.haversineM(l, p)
            val dt = (p.timeMs - l.timeMs) / 1000.0
            if (dt > 0 && d / dt > Track.MAX_SPEED_MPS) return 0.0 // a GPS jump, not a runner: skip this fix
            added = if (dt > 0) d else 0.0
        }
        last = p
        distanceM += added
        return added
    }

    /** Speed between the last two accepted fixes would be computed by the caller; this only forgets the position. */
    fun reset() { last = null; distanceM = 0.0 }
}

object Track {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val LAP_M = 400.0       // one lap of an athletics track
    const val MAX_SPEED_MPS = 12.0        // faster than that between two fixes is a GPS jump, not a runner

    fun haversineM(a: TrackPoint, b: TrackPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    /** Distance of a recorded track, ignoring imprecise fixes and impossible jumps. */
    fun distanceM(points: List<TrackPoint>, maxAccuracyM: Float = 30f): Double {
        val acc = TrackAccumulator(maxAccuracyM)
        points.forEach { acc.add(it) }
        return acc.distanceM
    }

    /** Seconds per kilometre, or null when there is nothing to divide by yet. */
    fun paceSecPerKm(distanceM: Double, seconds: Double): Double? =
        if (distanceM < 10 || seconds <= 0) null else seconds / (distanceM / 1000)

    /** The pace over the last [windowMs] of a track (what a runner calls "my pace right now"). */
    fun recentPaceSecPerKm(points: List<TrackPoint>, windowMs: Long = 30_000, maxAccuracyM: Float = 30f): Double? {
        val good = points.filter { it.accuracyM <= maxAccuracyM }
        val end = good.lastOrNull() ?: return null
        val window = good.filter { end.timeMs - it.timeMs <= windowMs }
        if (window.size < 2) return null
        val d = distanceM(window, maxAccuracyM)
        return paceSecPerKm(d, (end.timeMs - window.first().timeMs) / 1000.0)
    }

    fun laps(distanceM: Double): Double = distanceM / LAP_M
}
