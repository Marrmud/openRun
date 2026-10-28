package org.openrun.core

/**
 * A session laid out to be read at a glance (D42, mockup 4 bis B): the warm-up, the block of phases that repeats with
 * how many times, what is left after the last full block, then the cool-down.
 * "2 × (run 1:30, walk 1:30, run 3:00, walk 3:00)" or "6 × (run 1:30, walk 2:00)".
 */
data class SessionLayout(val warmup: Phase?, val block: List<Phase>, val times: Int, val rest: List<Phase>, val cooldown: Phase?)

object SessionLayouts {
    fun of(workout: Workout): SessionLayout {
        val warmup = workout.phases.firstOrNull()?.takeIf { it.kind == PhaseKind.WARMUP }
        val cooldown = workout.phases.lastOrNull()?.takeIf { it.kind == PhaseKind.COOLDOWN && workout.phases.size > 1 }
        val middle = workout.phases.drop(if (warmup != null) 1 else 0).dropLast(if (cooldown != null) 1 else 0)
        // the shortest block that, repeated at least twice, gives the whole middle (a partial block may close it)
        for (p in 1..middle.size / 2) {
            if (middle.indices.all { middle[it] == middle[it % p] }) {
                val times = middle.size / p
                return SessionLayout(warmup, middle.take(p), times, middle.drop(times * p), cooldown)
            }
        }
        return SessionLayout(warmup, middle, 1, emptyList(), cooldown)
    }
}
