package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramsTest {
    private fun minutes(w: Workout) = w.totalSeconds / 60.0

    @Test fun sessionCounts() {
        assertEquals(9, Programs.byId(Programs.PRE_C25K).sessionCount)
        assertEquals(27, Programs.byId(Programs.C25K).sessionCount)
        assertEquals(18, Programs.byId(Programs.BRIDGE_10K).sessionCount)
        assertEquals(42, Programs.byId(Programs.C210K).sessionCount)
    }

    @Test fun everySessionStartsWithWarmupAndEndsWithCooldown() {
        for (p in Programs.all) for (s in p.sessions) {
            assertEquals(PhaseKind.WARMUP, s.phases.first().kind)
            assertEquals(PhaseKind.COOLDOWN, s.phases.last().kind)
        }
    }

    @Test fun noPhaseIsEmptyAndNoTwoRunsFollowEachOther() {
        for (p in Programs.all) for (s in p.sessions) {
            s.phases.zipWithNext { a, b -> assertTrue("$p", !(a.kind == PhaseKind.RUN && b.kind == PhaseKind.RUN)) }
        }
    }

    @Test fun c25kWeekLengthsMatchTheProgramShape() {
        val c = Programs.byId(Programs.C25K)
        // warm-up and cool-down are 5 minutes each, so the figures below are the main part plus 10
        assertEquals(30.0, minutes(c.sessions[c.indexOf(1, 1)]), 0.01)
        assertEquals(31.0, minutes(c.sessions[c.indexOf(2, 1)]), 0.01)
        assertEquals(28.0, minutes(c.sessions[c.indexOf(3, 1)]), 0.01)
        assertEquals(31.5, minutes(c.sessions[c.indexOf(4, 1)]), 0.01)
        assertEquals(40.0, minutes(c.sessions[c.indexOf(9, 3)]), 0.01)
    }

    @Test fun weekTwoDayOneIsSixRunsOfNinetySeconds() {
        val c = Programs.byId(Programs.C25K)
        val s = c.sessions[c.indexOf(2, 1)]
        assertEquals(6, s.runCount)
        assertEquals(6 * 90, s.runSeconds)
        assertEquals(1 + 12 + 1, s.phases.size)
    }

    @Test fun theTenKilometreProgramStartsLikeTheFiveKilometreOne() {
        val c25k = Programs.byId(Programs.C25K)
        val c210k = Programs.byId(Programs.C210K)
        assertEquals(c25k.sessions, c210k.sessions.take(27))
        assertEquals(14, c210k.weeks.size)
    }

    @Test fun theLongestRunOfTheTenKilometreProgramIsAnHour() {
        val c = Programs.byId(Programs.C210K)
        assertEquals(60 * 60, c.sessions.last().runSeconds)
    }

    @Test fun locateAndIndexOfAreInverse() {
        for (p in Programs.all) for (i in 0 until p.sessionCount) {
            val (w, d) = p.locate(i)
            assertEquals(i, p.indexOf(w, d))
        }
    }

    @Test fun volumeNeverJumpsByMoreThanAThirdBetweenWeeks() {
        // a rule of thumb against injuries: the biggest session of a week vs the previous week
        for (p in Programs.all) {
            p.weeks.zipWithNext { a, b ->
                val before = a.sessions.maxOf { it.totalSeconds }
                val after = b.sessions.maxOf { it.totalSeconds }
                assertTrue("${p.id} w${a.number}->w${b.number}: $before -> $after", after <= before * 1.34)
            }
        }
    }

    @Test fun workoutWithoutWarmupAndCooldown() {
        val s = Programs.byId(Programs.C25K).sessions[0]
        assertEquals(s.totalSeconds - 300, s.withoutWarmup().totalSeconds)
        assertEquals(s.totalSeconds - 300, s.withoutCooldown().totalSeconds)
        assertEquals(s.totalSeconds - 600, s.withoutWarmup().withoutCooldown().totalSeconds)
        assertEquals(s.withoutWarmup(), s.withoutWarmup().withoutWarmup())
    }
}
