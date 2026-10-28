package org.openrun.core

/** Where a session is: which phase, how far into it. Time only moves when the app asks, so pausing is not calling. */
data class RunState(val phaseIndex: Int = 0, val elapsedMs: Long = 0, val finished: Boolean = false)

/** [entered] lists the phases started by the move, in order, so each can be announced. */
data class Step(val state: RunState, val entered: List<Int>)

object WorkoutRunner {
    /** Under this, "previous" goes to the previous phase; over it, it restarts the current one. */
    const val RESTART_AFTER_MS = 3_000L

    fun start(workout: Workout): Step =
        Step(RunState(), if (workout.phases.isEmpty()) emptyList() else listOf(0))

    fun leftMs(workout: Workout, s: RunState): Long =
        if (s.finished) 0 else (workout.phases[s.phaseIndex].seconds * 1000L - s.elapsedMs).coerceAtLeast(0)

    /** Time passed: a long gap (the phone was busy) can cross several phases at once. */
    fun tick(workout: Workout, s: RunState, deltaMs: Long): Step {
        if (s.finished || deltaMs <= 0) return Step(s, emptyList())
        var index = s.phaseIndex
        var elapsed = s.elapsedMs + deltaMs
        val entered = mutableListOf<Int>()
        while (elapsed >= workout.phases[index].seconds * 1000L) {
            elapsed -= workout.phases[index].seconds * 1000L
            if (index == workout.phases.lastIndex) {
                return Step(RunState(index, workout.phases[index].seconds * 1000L, finished = true), entered)
            }
            index++
            entered += index
        }
        return Step(RunState(index, elapsed), entered)
    }

    /** Next phase now; after the last one, the session is finished. */
    fun next(workout: Workout, s: RunState): Step {
        if (s.finished) return Step(s, emptyList())
        if (s.phaseIndex == workout.phases.lastIndex) {
            return Step(RunState(s.phaseIndex, workout.phases[s.phaseIndex].seconds * 1000L, finished = true), emptyList())
        }
        val i = s.phaseIndex + 1
        return Step(RunState(i), listOf(i))
    }

    /** A few seconds into a phase, restarts it; early on, goes back to the previous one. */
    fun previous(workout: Workout, s: RunState): Step {
        val back = !s.finished && s.elapsedMs <= RESTART_AFTER_MS && s.phaseIndex > 0
        val i = if (back) s.phaseIndex - 1 else s.phaseIndex.coerceAtMost(workout.phases.lastIndex)
        return Step(RunState(i), listOf(i))
    }

    fun restartPhase(workout: Workout, s: RunState): Step {
        val i = s.phaseIndex.coerceAtMost(workout.phases.lastIndex)
        return Step(RunState(i), listOf(i))
    }

    /** Straight to the start of the phase at [index] (touched in the timeline), forward or back. */
    fun goTo(workout: Workout, index: Int): Step {
        val i = index.coerceIn(0, workout.phases.lastIndex)
        return Step(RunState(i), listOf(i))
    }

    /** Back to the first phase (the warm-up). */
    fun toStart(workout: Workout): Step = start(workout)

    /** Time spent since the beginning, to show next to the session. */
    fun elapsedMs(workout: Workout, s: RunState): Long =
        workout.phases.take(s.phaseIndex).sumOf { it.seconds * 1000L } + s.elapsedMs
}
