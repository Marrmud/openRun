package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutTest {
    private fun run(s: Int) = Phase(PhaseKind.RUN, s)
    private fun walk(s: Int) = Phase(PhaseKind.WALK, s)

    @Test fun aSimpleSessionIsOneBlockRepeated() {
        val w = workout { warmup(); repeat(6) { run(90); walk(120) }; cooldown() }
        val l = SessionLayouts.of(w)
        assertEquals(listOf(run(90), walk(120)), l.block)
        assertEquals(6, l.times)
        assertEquals(emptyList<Phase>(), l.rest)
        assertEquals(PhaseKind.WARMUP, l.warmup?.kind)
        assertEquals(PhaseKind.COOLDOWN, l.cooldown?.kind)
    }

    @Test fun mixedIntervalsKeepTheirOrderInTheBlock() {
        val w = workout { warmup(); repeat(2) { run(90); walk(90); run(180); walk(180) }; cooldown() }
        val l = SessionLayouts.of(w)
        assertEquals(listOf(run(90), walk(90), run(180), walk(180)), l.block)
        assertEquals(2, l.times)
    }

    @Test fun aLastPartialBlockIsLeftAfterTheRepeats() {
        val w = workout { warmup(); repeat(5) { run(60); walk(90) }; run(60); cooldown() }
        val l = SessionLayouts.of(w)
        assertEquals(5, l.times)
        assertEquals(listOf(run(60)), l.rest)
    }

    @Test fun nothingRepeatsSoTheWholeMiddleIsOneBlock() {
        val w = workout { warmup(); run(300); walk(180); run(480); cooldown() }
        val l = SessionLayouts.of(w)
        assertEquals(1, l.times)
        assertEquals(3, l.block.size)
    }

    @Test fun withoutWarmupOrCooldown() {
        val l = SessionLayouts.of(workout { repeat(3) { run(60); walk(60) } })
        assertNull(l.warmup)
        assertNull(l.cooldown)
        assertEquals(3, l.times)
    }
}
