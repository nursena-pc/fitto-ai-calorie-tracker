package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_exercise.data.ExerciseDailySummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiSnapshotResolverTest {

    @Test
    fun resolve_buildsSnapshotFromSummaryState() {
        val targets = DailyNutritionTargetResolver.Targets(
            dailyCalories = 1800,
            proteinGrams = 100,
            carbGrams = 240,
            fatGrams = 60
        )
        val summaryState = DailySummaryUiStateResolver.resolve(
            nutritionSummary = DailyNutritionSummary(
                totalCalories = 1500,
                totalProtein = 80.0,
                totalCarbs = 120.0,
                totalFat = 45.0
            ),
            nutritionTargets = targets,
            exerciseSummary = ExerciseDailySummary(
                totalCalories = 120,
                totalDurationMinutes = 35,
                logCount = 2
            ),
            healthActiveCalories = 0,
            displaySteps = 5000,
            weightKg = 70.0
        )

        val snapshot = DailyAiSnapshotResolver.resolve(
            dateIso = "2026-06-25",
            userName = "Nursena",
            goalType = "LOSE_WEIGHT",
            nutritionTargets = targets,
            summaryState = summaryState,
            waterMl = 2000,
            waterGoalMl = 2000,
            meals = listOf(
                mealSnapshot(
                    mealTypeLabel = "Lunch",
                    totalCalories = 640,
                    proteinGrams = 35.0
                )
            ),
            exerciseLogCount = 2,
            exerciseSummaryText = "Walk: 35 min, 120 kcal",
            dietPreferences = "Balanced",
            allergies = "None",
            dislikedFoods = "None",
            mealRoutine = "Three meals",
            snackMealLabel = "Snack"
        )

        assertEquals("2026-06-25", snapshot.dateIso)
        assertEquals("Nursena", snapshot.userName)
        assertEquals("LOSE_WEIGHT", snapshot.goalType)
        assertEquals(1800, snapshot.dailyCalorieTarget)
        assertEquals(1500, snapshot.totalCalories)
        assertEquals(80.0, snapshot.proteinGrams, 0.0)
        assertEquals(100, snapshot.proteinTargetGrams)
        assertEquals(120.0, snapshot.carbGrams, 0.0)
        assertEquals(45.0, snapshot.fatGrams, 0.0)
        assertEquals(2000, snapshot.waterMl)
        assertEquals(2000, snapshot.waterGoalMl)
        assertEquals(AiDailyTriggerType.NORMAL, snapshot.triggerType)
        assertEquals(1, snapshot.meals.size)
        assertEquals(2, snapshot.exerciseLogCount)
        assertEquals(35, snapshot.exerciseDurationMinutes)
        assertEquals(340, snapshot.exerciseCalories)
        assertEquals("Walk: 35 min, 120 kcal", snapshot.exerciseSummaryText)
        assertEquals("Balanced", snapshot.dietPreferences)
        assertEquals("None", snapshot.allergies)
        assertEquals("None", snapshot.dislikedFoods)
        assertEquals("Three meals", snapshot.mealRoutine)
    }

    @Test
    fun resolve_usesTriggerResolverInputsFromSummaryState() {
        val targets = DailyNutritionTargetResolver.Targets(
            dailyCalories = 1800,
            proteinGrams = 100,
            carbGrams = 240,
            fatGrams = 60
        )
        val summaryState = DailySummaryUiStateResolver.resolve(
            nutritionSummary = DailyNutritionSummary(
                totalCalories = 2200,
                totalProtein = 80.0,
                totalCarbs = 120.0,
                totalFat = 45.0
            ),
            nutritionTargets = targets,
            exerciseSummary = ExerciseDailySummary(
                totalCalories = 0,
                totalDurationMinutes = 0,
                logCount = 0
            ),
            healthActiveCalories = 0,
            displaySteps = 0,
            weightKg = 70.0
        )

        val snapshot = DailyAiSnapshotResolver.resolve(
            dateIso = "2026-06-25",
            userName = "Nursena",
            goalType = "LOSE_WEIGHT",
            nutritionTargets = targets,
            summaryState = summaryState,
            waterMl = 2000,
            waterGoalMl = 2000,
            meals = listOf(
                mealSnapshot(
                    mealTypeLabel = "Lunch",
                    totalCalories = 700,
                    proteinGrams = 35.0
                )
            ),
            exerciseLogCount = 0,
            exerciseSummaryText = "",
            dietPreferences = "",
            allergies = "",
            dislikedFoods = "",
            mealRoutine = "",
            snackMealLabel = "Snack"
        )

        assertEquals(AiDailyTriggerType.CALORIE_EXCEEDED, snapshot.triggerType)
    }

    private fun mealSnapshot(
        mealTypeLabel: String,
        totalCalories: Int,
        proteinGrams: Double
    ): DailyAiMealSnapshot {
        return DailyAiMealSnapshot(
            mealTypeLabel = mealTypeLabel,
            totalCalories = totalCalories,
            proteinGrams = proteinGrams,
            carbGrams = 0.0,
            fatGrams = 0.0,
            entryCount = 1,
            foodSummary = "Food"
        )
    }
}
