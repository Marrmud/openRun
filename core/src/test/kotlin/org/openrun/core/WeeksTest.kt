package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeksTest {
    // 2026-10-25 is a Sunday; the week is Mon 19 to Sun 25
    private val sunday = LocalDate.of(2026, 10, 25)
    private fun d(day: Int, month: Int = 10) = LocalDate.of(2026, month, day)

    @Test fun weeksStartOnMonday() {
        assertEquals(d(19), Weeks.startOfWeek(sunday))
        assertEquals(d(19), Weeks.startOfWeek(d(19)))
        assertEquals(d(19), Weeks.startOfWeek(d(22)))
        assertEquals(d(26), Weeks.startOfWeek(d(26)))
    }

    @Test fun aWeekHasSevenDaysFromMonday() {
        val days = Weeks.daysOf(sunday)
        assertEquals(7, days.size)
        assertEquals(d(19), days.first())
        assertEquals(d(25), days.last())
    }

    @Test fun runsOfTheWeekAreCounted() {
        val runs = listOf(d(17), d(19), d(22), d(24), d(26))
        assertEquals(3, Weeks.runsInWeekOf(sunday, runs))
    }

    @Test fun noRunsNoStreak() {
        assertEquals(0, Weeks.streak(emptyList(), 3, sunday))
    }

    @Test fun theWeekInProgressNeverBreaksTheStreak() {
        // last week met the goal (3), this week has only 1 so far
        val runs = listOf(d(12), d(14), d(17), d(19))
        assertEquals(1, Weeks.streak(runs, 3, sunday))
    }

    @Test fun theWeekInProgressAddsOnceTheGoalIsMet() {
        val runs = listOf(d(12), d(14), d(17), d(19), d(22), d(24))
        assertEquals(2, Weeks.streak(runs, 3, sunday))
    }

    @Test fun aMissedWeekBreaksIt() {
        // two weeks ago ok, last week only 2, this week 3
        val runs = listOf(d(5), d(7), d(10), d(14), d(17), d(19), d(22), d(24))
        assertEquals(1, Weeks.streak(runs, 3, sunday))
    }

    @Test fun aRestDayInsideAWeekChangesNothing() {
        // 3 runs bunched at the start of the week still count the same
        assertEquals(1, Weeks.streak(listOf(d(19), d(20), d(21)), 3, sunday))
    }

    @Test fun theGoalIsConfigurable() {
        val runs = listOf(d(12), d(19))
        assertEquals(2, Weeks.streak(runs, 1, sunday))
        assertEquals(0, Weeks.streak(runs, 2, sunday))
    }
}
