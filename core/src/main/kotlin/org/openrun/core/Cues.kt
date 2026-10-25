package org.openrun.core

/** When the app speaks, beeps or counts a session as done: small rules, kept apart so they can be tested. */
object Cues {
    /** Which of 3, 2, 1 were passed between two readings of the time left in a phase (usually none or one). */
    fun countdownCrossed(prevLeftMs: Long, leftMs: Long, from: Int = 3): List<Int> =
        (from downTo 1).filter { n -> prevLeftMs > n * 1000L && leftMs <= n * 1000L }

    /** The minute count when a time reminder is due (every 5 minutes by default), else null. */
    fun reminderCrossed(prevElapsedMs: Long, elapsedMs: Long, everyMs: Long = 300_000): Int? {
        if (everyMs <= 0 || elapsedMs < everyMs) return null
        return if (prevElapsedMs / everyMs < elapsedMs / everyMs) ((elapsedMs / everyMs) * everyMs / 60_000).toInt() else null
    }

    /**
     * Whether a session saved before the end still counts as done in its program: when it ran to the end, or when at
     * least [threshold] of the planned time was covered (phases skipped count as covered).
     */
    fun countsAsDone(positionMs: Long, plannedMs: Long, finished: Boolean, threshold: Double = 0.8): Boolean =
        finished || (plannedMs > 0 && positionMs >= plannedMs * threshold)

    /** Halfway through the plan: the moment for one word of encouragement. */
    fun halfwayCrossed(prevPositionMs: Long, positionMs: Long, plannedMs: Long): Boolean =
        plannedMs > 0 && prevPositionMs < plannedMs / 2 && positionMs >= plannedMs / 2
}
