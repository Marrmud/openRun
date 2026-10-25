package org.openrun.core

import java.time.Instant
import java.time.ZoneId

/** A past session as the history lists it. [completed] is true when it counted for its program session. */
data class HistoryItem(
    val id: Long,
    val startedAt: Long,
    val programId: String?,
    val sessionIndex: Int?,
    val completed: Boolean,
    val durationSec: Int,
    val distanceM: Double,
    val kcal: Double,
    /** True when a watch recorded it, or added readings (heart rate) the phone cannot measure. */
    val hasWatchData: Boolean,
)

data class MonthGroup(val year: Int, val month: Int, val items: List<HistoryItem>)

data class ProgramGroup(val programId: String?, val items: List<HistoryItem>) {
    val completedCount: Int get() = items.count { it.completed }
    val distanceM: Double get() = items.sumOf { it.distanceM }
}

data class HistoryTotals(val runs: Int, val distanceM: Double, val durationSec: Long)

object History {
    private fun newestFirst(items: List<HistoryItem>) = items.sortedByDescending { it.startedAt }

    fun byMonth(items: List<HistoryItem>, zone: ZoneId): List<MonthGroup> =
        newestFirst(items)
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).let { d -> d.year to d.monthValue } }
            .map { (ym, list) -> MonthGroup(ym.first, ym.second, list) }

    /** One group per program (free runs together, last), the program with the most recent session first. */
    fun byProgram(items: List<HistoryItem>): List<ProgramGroup> =
        newestFirst(items)
            .groupBy { it.programId }
            .map { (id, list) -> ProgramGroup(id, list) }
            .sortedWith(compareBy<ProgramGroup> { it.programId == null }.thenByDescending { it.items.first().startedAt })

    fun totals(items: List<HistoryItem>) =
        HistoryTotals(items.size, items.sumOf { it.distanceM }, items.sumOf { it.durationSec.toLong() })
}
