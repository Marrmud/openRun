package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaloriesTest {
    @Test fun runningCostsAboutOneKcalPerKgPerKm() {
        // 70 kg, 10 km in an hour
        val kcal = Calories.kcal(70.0, 10_000.0 / 3600, 3600.0, running = true)
        assertEquals(700.0, kcal, 15.0)
    }

    @Test fun walkingCostsLessThanRunningAtTheSameSpeedPerMinute() {
        val run = Calories.kcal(70.0, 2.0, 600.0, running = true)
        val walk = Calories.kcal(70.0, 2.0, 600.0, running = false)
        assertEquals(run / 2, walk, 0.01)
    }

    @Test fun nothingWithoutWeightOrTime() {
        assertEquals(0.0, Calories.kcal(0.0, 2.0, 600.0, true), 0.0)
        assertEquals(0.0, Calories.kcal(70.0, 2.0, 0.0, true), 0.0)
    }

    @Test fun segmentsUseTheDistanceWhenThereIsOne() {
        val withDistance = Calories.kcal(70.0, listOf(Calories.Segment(true, 600.0, 2400.0)))
        val byDefault = Calories.kcal(70.0, listOf(Calories.Segment(true, 600.0)))
        assertTrue(withDistance > byDefault) // 4 m/s beats the 2.3 m/s default
    }

    @Test fun anImpossibleGpsSpeedIsCapped() {
        val crazy = Calories.kcal(70.0, listOf(Calories.Segment(true, 60.0, 5000.0)))
        assertEquals(Calories.kcal(70.0, 7.0, 60.0, true), crazy, 0.001)
    }

    @Test fun aPlannedSessionIsWorthSomethingReasonable() {
        val c25kWeek2 = Programs.byId(Programs.C25K).sessions[3]
        val kcal = Calories.planned(70.0, c25kWeek2)
        assertTrue("got $kcal", kcal in 150.0..400.0)
    }
}
