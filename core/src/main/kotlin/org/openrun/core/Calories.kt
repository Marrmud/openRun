package org.openrun.core

/**
 * Calories actually spent moving (not counting what the body burns at rest), from the ACSM walking and running
 * equations. About 1 kcal per kg per km when running, which is what the watch reports within a few percent.
 * With a watch, its figure replaces this one; this is for a phone alone.
 */
object Calories {
    const val DEFAULT_RUN_MPS = 2.3   // about 8.3 km/h, a beginner's pace
    const val DEFAULT_WALK_MPS = 1.4  // about 5 km/h
    private const val MAX_MPS = 7.0

    fun kcal(weightKg: Double, speedMps: Double, seconds: Double, running: Boolean): Double {
        if (weightKg <= 0 || seconds <= 0) return 0.0
        val metersPerMin = speedMps.coerceIn(0.0, MAX_MPS) * 60
        val vo2 = (if (running) 0.2 else 0.1) * metersPerMin // ml of oxygen per kg per minute
        return vo2 * weightKg * (seconds / 60) * 0.005       // 1 litre of oxygen is about 5 kcal
    }

    /** One stretch of a session. Without a distance (no GPS, treadmill), a typical speed stands in. */
    data class Segment(val running: Boolean, val seconds: Double, val distanceM: Double? = null)

    fun kcal(weightKg: Double, segments: List<Segment>): Double = segments.sumOf {
        val speed = if (it.distanceM != null && it.seconds > 0) it.distanceM / it.seconds
        else if (it.running) DEFAULT_RUN_MPS else DEFAULT_WALK_MPS
        kcal(weightKg, speed, it.seconds, it.running)
    }

    /** What a planned workout is worth for a runner of this weight, with typical speeds. */
    fun planned(weightKg: Double, workout: Workout): Double = kcal(weightKg, workout.phases.map {
        Segment(running = it.kind == PhaseKind.RUN, seconds = it.seconds.toDouble())
    })
}
