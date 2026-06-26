package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_exercise.data.ExerciseDailySummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryUiStateResolverTest {

    @Test
    fun resolve_buildsEnergyActivityAndMacroProgressState() {
        val state = DailySummaryUiStateResolver.resolve(
            nutritionSummary = DailyNutritionSummary(
                totalCalories = 1500,
                totalProtein = 80.0,
                totalCarbs = 120.0,
                totalFat = 45.0
            ),
            nutritionTargets = DailyNutritionTargetResolver.Targets(
                dailyCalories = 1800,
                proteinGrams = 100,
                carbGrams = 240,
                fatGrams = 60
            ),
            exerciseSummary = ExerciseDailySummary(
                totalCalories = 120,
                totalDurationMinutes = 35,
                logCount = 2
            ),
            healthActiveCalories = 0,
            displaySteps = 5000,
            weightKg = 70.0
        )

        assertEquals(1500, state.energyBalance.eatenCalories)
        assertEquals(120, state.energyBalance.manualExerciseCalories)
        assertEquals(220, state.energyBalance.syncedActivityCalories)
        assertEquals(340, state.energyBalance.burnedCalories)
        assertEquals(640, state.energyBalance.remainingCalories)
        assertEquals(5000, state.displaySteps)
        assertEquals(35, state.activityDurationMinutes)
        assertEquals(83, state.calorieProgressPercent)
        assertEquals(
            DailySummaryUiStateResolver.MacroState(
                currentGrams = 120.0,
                targetGrams = 240,
                progressPercent = 50
            ),
            state.carbs
        )
        assertEquals(
            DailySummaryUiStateResolver.MacroState(
                currentGrams = 80.0,
                targetGrams = 100,
                progressPercent = 80
            ),
            state.protein
        )
        assertEquals(
            DailySummaryUiStateResolver.MacroState(
                currentGrams = 45.0,
                targetGrams = 60,
                progressPercent = 75
            ),
            state.fat
        )
    }

    @Test
    fun resolve_clampsNegativeExerciseDuration() {
        val state = DailySummaryUiStateResolver.resolve(
            nutritionSummary = DailyNutritionSummary(
                totalCalories = 0,
                totalProtein = 0.0,
                totalCarbs = 0.0,
                totalFat = 0.0
            ),
            nutritionTargets = DailyNutritionTargetResolver.Targets(
                dailyCalories = 1800,
                proteinGrams = 100,
                carbGrams = 240,
                fatGrams = 60
            ),
            exerciseSummary = ExerciseDailySummary(
                totalCalories = 0,
                totalDurationMinutes = -5,
                logCount = 0
            ),
            healthActiveCalories = 0,
            displaySteps = 0,
            weightKg = 70.0
        )

        assertEquals(0, state.activityDurationMinutes)
    }
}
