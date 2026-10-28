package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunnerTest {
    // warm-up 10 s, run 20 s, walk 20 s, run 20 s, cool-down 10 s
    private val w = Workout(listOf(
        Phase(PhaseKind.WARMUP, 10), Phase(PhaseKind.RUN, 20), Phase(PhaseKind.WALK, 20),
        Phase(PhaseKind.RUN, 20), Phase(PhaseKind.COOLDOWN, 10),
    ))

    @Test fun startAnnouncesTheFirstPhase() {
        val s = WorkoutRunner.start(w)
        assertEquals(listOf(0), s.entered)
        assertEquals(10_000, WorkoutRunner.leftMs(w, s.state))
    }

    @Test fun tickInsideAPhaseEntersNothing() {
        val s = WorkoutRunner.tick(w, RunState(), 4_000)
        assertTrue(s.entered.isEmpty())
        assertEquals(6_000, WorkoutRunner.leftMs(w, s.state))
    }

    @Test fun tickCrossesIntoTheNextPhase() {
        val s = WorkoutRunner.tick(w, RunState(0, 9_000), 2_000)
        assertEquals(listOf(1), s.entered)
        assertEquals(RunState(1, 1_000), s.state)
    }

    @Test fun aLongGapCrossesSeveralPhasesInOrder() {
        val s = WorkoutRunner.tick(w, RunState(), 55_000)
        assertEquals(listOf(1, 2, 3), s.entered)
        assertEquals(RunState(3, 5_000), s.state)
    }

    @Test fun reachingTheEndFinishes() {
        val s = WorkoutRunner.tick(w, RunState(4, 9_000), 2_000)
        assertTrue(s.state.finished)
        assertEquals(0, WorkoutRunner.leftMs(w, s.state))
        assertEquals(80_000, WorkoutRunner.elapsedMs(w, s.state)) // the whole session: 10+20+20+20+10 s
    }

    @Test fun aFinishedSessionStaysFinished() {
        val done = WorkoutRunner.tick(w, RunState(4, 9_000), 2_000).state
        val again = WorkoutRunner.tick(w, done, 5_000)
        assertEquals(done, again.state)
        assertTrue(again.entered.isEmpty())
    }

    @Test fun nextSkipsThePhase() {
        val s = WorkoutRunner.next(w, RunState(1, 7_000))
        assertEquals(listOf(2), s.entered)
        assertEquals(RunState(2, 0), s.state)
    }

    @Test fun nextOnTheLastPhaseFinishes() {
        val s = WorkoutRunner.next(w, RunState(4, 2_000))
        assertTrue(s.state.finished)
    }

    @Test fun previousEarlyGoesBackToThePreviousPhase() {
        val s = WorkoutRunner.previous(w, RunState(2, 1_000))
        assertEquals(RunState(1, 0), s.state)
        assertEquals(listOf(1), s.entered)
    }

    @Test fun previousLateRestartsThePhase() {
        val s = WorkoutRunner.previous(w, RunState(2, 8_000))
        assertEquals(RunState(2, 0), s.state)
    }

    @Test fun previousOnTheFirstPhaseRestartsIt() {
        val s = WorkoutRunner.previous(w, RunState(0, 1_000))
        assertEquals(RunState(0, 0), s.state)
    }

    @Test fun previousAfterTheEndReopensTheLastPhase() {
        val done = WorkoutRunner.next(w, RunState(4, 0)).state
        assertTrue(done.finished)
        val s = WorkoutRunner.previous(w, done)
        assertFalse(s.state.finished)
        assertEquals(4, s.state.phaseIndex)
    }

    @Test fun goToJumpsForwardOrBackToTheStartOfAPhase() {
        assertEquals(Step(RunState(3), listOf(3)), WorkoutRunner.goTo(w, 3))
        assertEquals(Step(RunState(1), listOf(1)), WorkoutRunner.goTo(w, 1))
        assertEquals(4, WorkoutRunner.goTo(w, 99).state.phaseIndex)
    }

    @Test fun backToTheWarmupFromAnywhere() {
        assertEquals(RunState(0, 0), WorkoutRunner.toStart(w).state)
    }

    @Test fun elapsedAddsThePhasesBeforeAndTheTimeInTheCurrentOne() {
        assertEquals(10_000 + 20_000 + 3_000, WorkoutRunner.elapsedMs(w, RunState(2, 3_000)))
    }
}
