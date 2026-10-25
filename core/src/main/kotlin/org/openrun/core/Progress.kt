package org.openrun.core

/** What picking a day of a program changes: sessions marked done, sessions made to do again. */
data class JumpPlan(val target: Int, val markDone: List<Int>, val unmarkDone: List<Int>) {
    val isNoop: Boolean get() = markDone.isEmpty() && unmarkDone.isEmpty()
}

/** Which sessions of a program are done. The current one is always the first that is not. */
data class ProgramProgress(val sessionCount: Int, val done: Set<Int> = emptySet()) {
    /** Null once the whole program is done. */
    val currentIndex: Int? get() = (0 until sessionCount).firstOrNull { it !in done }
    val isFinished: Boolean get() = currentIndex == null

    fun complete(index: Int): ProgramProgress {
        require(index in 0 until sessionCount)
        return copy(done = done + index)
    }

    /**
     * Starting at [target]: going forward, every session before it counts as done; going back, [target] and every
     * session after it are to do again. Either way [target] becomes the current session.
     */
    fun planJump(target: Int): JumpPlan {
        require(target in 0 until sessionCount)
        val current = currentIndex ?: sessionCount
        return if (target > current) {
            JumpPlan(
                target,
                markDone = (0 until target).filter { it !in done },
                unmarkDone = listOf(target).filter { it in done },
            )
        } else {
            JumpPlan(target, markDone = emptyList(), unmarkDone = done.filter { it >= target }.sorted())
        }
    }

    fun apply(plan: JumpPlan): ProgramProgress =
        copy(done = done + plan.markDone - plan.unmarkDone.toSet())
}
