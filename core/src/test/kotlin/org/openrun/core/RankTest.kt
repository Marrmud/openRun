package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RankTest {
    @Test fun startsAtTheBottom() {
        val r = Ranks.forTotal(0.0)
        assertEquals(RankTier.FER, r.tier)
        assertEquals(3, r.division)
        assertEquals(0f, r.progress, 0f)
    }

    @Test fun staysInTheDivisionUntilItIsFull() {
        assertEquals(3, Ranks.forTotal(999.0).division)
        val r = Ranks.forTotal(1000.0)
        assertEquals(RankTier.FER, r.tier)
        assertEquals(2, r.division)
        assertEquals(0, r.kcalInDivision)
    }

    @Test fun threeDivisionsPerTier() {
        assertEquals(RankTier.FER, Ranks.forTotal(2999.0).tier)
        assertEquals(1, Ranks.forTotal(2999.0).division)
        val bronze = Ranks.forTotal(3000.0)
        assertEquals(RankTier.BRONZE, bronze.tier)
        assertEquals(3, bronze.division)
    }

    @Test fun theMockupExample() {
        // "Bronze II, 620 / 1 000 kcal"
        val r = Ranks.forTotal(4620.0)
        assertEquals(RankTier.BRONZE, r.tier)
        assertEquals(2, r.division)
        assertEquals(620, r.kcalInDivision)
        assertEquals(0.62f, r.progress, 0.0001f)
        assertEquals(380, r.kcalToNext)
    }

    @Test fun diamondOneIsTheTopAndStaysThere() {
        val first = Ranks.forTotal(17_000.0)
        assertEquals(RankTier.DIAMANT, first.tier)
        assertEquals(3, Ranks.forTotal(15_000.0).division)
        val top = Ranks.forTotal(1_000_000.0)
        assertEquals(RankTier.DIAMANT, top.tier)
        assertEquals(1, top.division)
        assertEquals(1f, top.progress, 0f)
    }

    @Test fun negativeTotalsAreZero() {
        assertEquals(RankTier.FER, Ranks.forTotal(-50.0).tier)
    }

    @Test fun theDivisionSizeIsOneConstant() {
        val r = Ranks.forTotal(250.0, kcalPerDivision = 500)
        assertEquals(0.5f, r.progress, 0.0001f)
        assertEquals(RankTier.BRONZE, Ranks.forTotal(1500.0, kcalPerDivision = 500).tier)
    }
}
