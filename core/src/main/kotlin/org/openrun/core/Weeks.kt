package org.openrun.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The weekly goal. Weeks start on Monday. A streak counts weeks that met the goal, not days: a rest day breaks
 * nothing, and the week in progress never breaks it (it only adds to it once the goal is met).
 */
object Weeks {
    fun startOfWeek(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Monday to Sunday of the week of [d]. */
    fun daysOf(d: LocalDate): List<LocalDate> = startOfWeek(d).let { m -> List(7) { m.plusDays(it.toLong()) } }

    fun runsInWeekOf(d: LocalDate, runDates: List<LocalDate>): Int {
        val start = startOfWeek(d)
        return runDates.count { startOfWeek(it) == start }
    }

    fun streak(runDates: List<LocalDate>, goal: Int, today: LocalDate): Int {
        require(goal >= 1)
        val perWeek = runDates.groupingBy { startOfWeek(it) }.eachCount()
        var cursor = startOfWeek(today)
        var streak = 0
        if ((perWeek[cursor] ?: 0) >= goal) streak++
        cursor = cursor.minusWeeks(1)
        while ((perWeek[cursor] ?: 0) >= goal) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        return streak
    }
}
