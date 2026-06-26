package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyEnergyBalanceCalculatorTest {

    @Test
    fun calculate_withoutHealthCalories_usesStepCalories() {
        val result = DailyEnergyBalanceCalculator.calculate(
            targetCalories = 1800,
            eatenCalories = 1500,
            manualExerciseCalories = 100,
            healthActiveCalories = 0,
            displaySteps = 5000,
            weightKg = 70.0
        )

        assertEquals(1500, result.eatenCalories)
        assertEquals(100, result.manualExerciseCalories)
        assertEquals(220, result.stepCalories)
        assertEquals(220, result.syncedActivityCalories)
        assertEquals(320, result.burnedCalories)
        assertEquals(620, result.remainingCalories)
    }

    @Test
    fun calculate_withHealthCalories_ignoresStepCalories() {
        val result = DailyEnergyBalanceCalculator.calculate(
            targetCalories = 1800,
            eatenCalories = 1500,
            manualExerciseCalories = 100,
            healthActiveCalories = 350,
            displaySteps = 5000,
            weightKg = 70.0
        )

        assertEquals(0, result.stepCalories)
        assertEquals(350, result.syncedActivityCalories)
        assertEquals(450, result.burnedCalories)
        assertEquals(750, result.remainingCalories)
    }

    @Test
    fun calculate_clampsNegativeInputsAndRemainingCalories() {
        val result = DailyEnergyBalanceCalculator.calculate(
            targetCalories = 1200,
            eatenCalories = 2000,
            manualExerciseCalories = -10,
            healthActiveCalories = -20,
            displaySteps = -500,
            weightKg = 70.0
        )

        assertEquals(2000, result.eatenCalories)
        assertEquals(0, result.manualExerciseCalories)
        assertEquals(0, result.stepCalories)
        assertEquals(0, result.syncedActivityCalories)
        assertEquals(0, result.burnedCalories)
        assertEquals(0, result.remainingCalories)
    }
}
