package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryTest {
    private val utc = ZoneId.of("UTC")
    private fun ms(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atTime(12, 0).atZone(utc).toInstant().toEpochMilli()
    private fun item(id: Long, at: Long, program: String? = null, session: Int? = null, done: Boolean = false, km: Double = 3000.0) =
        HistoryItem(id, at, program, session, done, 1800, km, 250.0, false)

    @Test fun monthsAreNewestFirstAndSplitAtTheMonthBoundary() {
        val items = listOf(item(1, ms(2026, 9, 30)), item(2, ms(2026, 10, 2)), item(3, ms(2026, 10, 24)))
        val groups = History.byMonth(items, utc)
        assertEquals(listOf(2026 to 10, 2026 to 9), groups.map { it.year to it.month })
        assertEquals(listOf(3L, 2L), groups[0].items.map { it.id })
        assertEquals(listOf(1L), groups[1].items.map { it.id })
    }

    @Test fun theLocalTimeZoneDecidesWhichMonthASessionBelongsTo() {
        // 23:30 on 31 October in UTC is already 1 November in Paris-like UTC+1
        val late = LocalDate.of(2026, 10, 31).atTime(23, 30).atZone(utc).toInstant().toEpochMilli()
        assertEquals(10, History.byMonth(listOf(item(1, late)), utc).single().month)
        assertEquals(11, History.byMonth(listOf(item(1, late)), ZoneId.of("Europe/Paris")).single().month)
    }

    @Test fun programsAreGroupedWithFreeRunsLast() {
        val items = listOf(
            item(1, ms(2026, 10, 1), program = Programs.C25K, session = 0, done = true),
            item(2, ms(2026, 10, 20)),                                         // free run, the most recent
            item(3, ms(2026, 10, 10), program = Programs.C25K, session = 1, done = false),
            item(4, ms(2026, 9, 1), program = Programs.PRE_C25K, session = 0, done = true),
        )
        val groups = History.byProgram(items)
        assertEquals(listOf(Programs.C25K, Programs.PRE_C25K, null), groups.map { it.programId })
        assertEquals(1, groups[0].completedCount)
        assertEquals(2, groups[0].items.size)
        assertEquals(listOf(3L, 1L), groups[0].items.map { it.id })
    }

    @Test fun totalsAddUp() {
        val t = History.totals(listOf(item(1, 1, km = 2000.0), item(2, 2, km = 3500.0)))
        assertEquals(2, t.runs)
        assertEquals(5500.0, t.distanceM, 0.0)
        assertEquals(3600L, t.durationSec)
    }

    @Test fun nothingInNothingOut() {
        assertTrue(History.byMonth(emptyList(), utc).isEmpty())
        assertTrue(History.byProgram(emptyList()).isEmpty())
        assertEquals(0, History.totals(emptyList()).runs)
    }

    @Test fun promotionHappensOnlyAcrossADivisionBoundary() {
        assertNull(Ranks.promotion(100.0, 900.0))
        val p = Ranks.promotion(900.0, 1100.0)
        assertNotNull(p)
        assertEquals(RankTier.FER, p!!.tier); assertEquals(2, p.division)
        assertEquals(RankTier.BRONZE, Ranks.promotion(2900.0, 3100.0)!!.tier)
    }

    @Test fun aSessionCanJumpSeveralDivisionsAndStillReportsTheNewRank() {
        val p = Ranks.promotion(500.0, 4200.0)!!
        assertEquals(RankTier.BRONZE, p.tier); assertEquals(2, p.division)
    }

    @Test fun noPromotionAtTheTopOrWhenTheTotalDrops() {
        assertNull(Ranks.promotion(50_000.0, 60_000.0))
        assertNull(Ranks.promotion(1100.0, 900.0))
    }
}
