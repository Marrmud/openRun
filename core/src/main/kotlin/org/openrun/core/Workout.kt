package org.openrun.core

enum class PhaseKind { WARMUP, RUN, WALK, COOLDOWN }

data class Phase(val kind: PhaseKind, val seconds: Int) {
    init { require(seconds > 0) { "a phase lasts at least a second" } }
}

/** A session of a program: a list of timed phases, in order. */
data class Workout(val phases: List<Phase>) {
    val totalSeconds: Int get() = phases.sumOf { it.seconds }
    val runSeconds: Int get() = phases.filter { it.kind == PhaseKind.RUN }.sumOf { it.seconds }
    val runCount: Int get() = phases.count { it.kind == PhaseKind.RUN }
    val walkCount: Int get() = phases.count { it.kind == PhaseKind.WALK }

    fun withoutWarmup(): Workout =
        if (phases.firstOrNull()?.kind == PhaseKind.WARMUP) Workout(phases.drop(1)) else this

    fun withoutCooldown(): Workout =
        if (phases.lastOrNull()?.kind == PhaseKind.COOLDOWN) Workout(phases.dropLast(1)) else this
}

class WorkoutBuilder {
    private val list = mutableListOf<Phase>()

    fun warmup(minutes: Int = 5) { list += Phase(PhaseKind.WARMUP, minutes * 60) }
    fun cooldown(minutes: Int = 5) { list += Phase(PhaseKind.COOLDOWN, minutes * 60) }
    fun run(seconds: Int) { list += Phase(PhaseKind.RUN, seconds) }
    fun walk(seconds: Int) { list += Phase(PhaseKind.WALK, seconds) }

    fun repeat(times: Int, block: WorkoutBuilder.() -> Unit) {
        for (i in 1..times) list += WorkoutBuilder().apply(block).list
    }

    internal fun build() = Workout(list.toList())
}

fun workout(block: WorkoutBuilder.() -> Unit): Workout = WorkoutBuilder().apply(block).build()
