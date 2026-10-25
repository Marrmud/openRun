package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private fun fresh() = ProgramProgress(27)

    @Test fun currentIsTheFirstSessionNotDone() {
        assertEquals(0, fresh().currentIndex)
        assertEquals(2, fresh().complete(0).complete(1).currentIndex)
    }

    @Test fun finishingEverythingFinishes() {
        var p = fresh()
        for (i in 0 until 27) p = p.complete(i)
        assertTrue(p.isFinished)
        assertNull(p.currentIndex)
    }

    @Test fun jumpingForwardMarksTheSkippedSessionsDone() {
        val p = fresh().complete(0)
        val plan = p.planJump(4)
        assertEquals(listOf(1, 2, 3), plan.markDone)
        assertTrue(plan.unmarkDone.isEmpty())
        val after = p.apply(plan)
        assertEquals(4, after.currentIndex)
        assertEquals(setOf(0, 1, 2, 3), after.done)
    }

    @Test fun jumpingBackMakesEverythingFromThereToDoAgain() {
        var p = fresh()
        for (i in 0 until 6) p = p.complete(i)
        val plan = p.planJump(2)
        assertEquals(listOf(2, 3, 4, 5), plan.unmarkDone)
        assertTrue(plan.markDone.isEmpty())
        assertEquals(2, p.apply(plan).currentIndex)
        assertEquals(setOf(0, 1), p.apply(plan).done)
    }

    @Test fun jumpingToTheCurrentSessionChangesNothing() {
        val p = fresh().complete(0).complete(1)
        assertTrue(p.planJump(2).isNoop)
        assertEquals(p, p.apply(p.planJump(2)))
    }

    @Test fun afterAnyJumpTheTargetIsTheCurrentSession() {
        // including out of order histories: session 5 done while 2 was skipped
        val p = fresh().complete(0).complete(1).complete(5)
        for (target in 0 until 27) {
            assertEquals("target $target", target, p.apply(p.planJump(target)).currentIndex)
        }
    }

    @Test fun jumpingForwardOntoASessionAlreadyDoneMakesItToDoAgain() {
        val p = fresh().complete(0).complete(5)
        val plan = p.planJump(5)
        assertEquals(listOf(5), plan.unmarkDone)
        assertFalse(5 in p.apply(plan).done)
    }

    @Test fun jumpingBeyondAFinishedProgramGoesBack() {
        var p = fresh()
        for (i in 0 until 27) p = p.complete(i)
        assertEquals(10, p.apply(p.planJump(10)).currentIndex)
    }
}
