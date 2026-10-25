package org.openrun.core

import kotlin.math.cos
import kotlin.math.max

/**
 * A track flattened for drawing, with no map under it. [points] are in 0..1 on both axes, north up, with the real
 * proportions kept: the longer side of the route spans 1, the other side [widthFraction] or [heightFraction].
 */
data class ProjectedTrack(val points: List<Pair<Float, Float>>, val widthFraction: Float, val heightFraction: Float)

object TrackShape {
    fun project(points: List<TrackPoint>, maxAccuracyM: Float = 50f): ProjectedTrack? {
        val good = points.filter { it.accuracyM <= maxAccuracyM }
        if (good.size < 2) return null
        val lat0 = Math.toRadians(good.map { it.lat }.average())
        val minLon = good.minOf { it.lon }
        val maxLat = good.maxOf { it.lat }
        val xs = good.map { (it.lon - minLon) * cos(lat0) }
        val ys = good.map { maxLat - it.lat }
        val spanX = xs.max()
        val spanY = ys.max()
        val scale = max(spanX, spanY)
        if (scale <= 0.0) return null
        return ProjectedTrack(
            points = xs.indices.map { (xs[it] / scale).toFloat() to (ys[it] / scale).toFloat() },
            widthFraction = (spanX / scale).toFloat(),
            heightFraction = (spanY / scale).toFloat(),
        )
    }
}
