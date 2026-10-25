package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuesTest {
    @Test fun countdownFiresOncePerSecondCrossed() {
        assertEquals(listOf(3), Cues.countdownCrossed(3_200, 2_950))
        assertEquals(listOf(2), Cues.countdownCrossed(2_100, 1_900))
        assertEquals(listOf(1), Cues.countdownCrossed(1_050, 800))
    }

    @Test fun countdownIsQuietOutsideTheLastThreeSeconds() {
        assertTrue(Cues.countdownCrossed(10_000, 9_750).isEmpty())
        assertTrue(Cues.countdownCrossed(2_900, 2_700).isEmpty())
    }

    @Test fun countdownCanCatchSeveralAfterAGap() {
        assertEquals(listOf(3, 2), Cues.countdownCrossed(3_500, 1_900))
    }

    @Test fun aNewPhaseNeverTriggersTheCountdown() {
        // the time left jumps up when the next phase begins
        assertTrue(Cues.countdownCrossed(400, 90_000).isEmpty())
    }

    @Test fun reminderEveryFiveMinutes() {
        assertNull(Cues.reminderCrossed(0, 299_000))
        assertEquals(5, Cues.reminderCrossed(299_900, 300_100))
        assertEquals(10, Cues.reminderCrossed(599_000, 601_000))
        assertNull(Cues.reminderCrossed(300_100, 301_000))
    }

    @Test fun halfwayFiresOnce() {
        assertTrue(Cues.halfwayCrossed(899_000, 901_000, 1_800_000))
        assertFalse(Cues.halfwayCrossed(901_000, 903_000, 1_800_000))
        assertFalse(Cues.halfwayCrossed(0, 100, 0))
    }

    @Test fun aFinishedSessionCountsAsDone() {
        assertTrue(Cues.countsAsDone(positionMs = 1_000, plannedMs = 1_800_000, finished = true))
    }

    @Test fun anEarlyEndCountsOnlyFromEightyPercent() {
        assertFalse(Cues.countsAsDone(1_400_000, 1_800_000, false)) // 78 %
        assertTrue(Cues.countsAsDone(1_440_000, 1_800_000, false))  // 80 %
        assertFalse(Cues.countsAsDone(100, 0, false))               // a free run has no plan
    }
}
