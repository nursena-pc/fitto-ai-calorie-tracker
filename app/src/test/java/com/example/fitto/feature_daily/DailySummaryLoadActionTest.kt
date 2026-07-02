package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_exercise.data.ExerciseDailySummary
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_health.HealthConnectDailySummary
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailySummaryLoadActionTest {

    @Test
    fun `loads daily sources and resolves dashboard summary`() = runBlocking {
        val requestedMealTypes = mutableListOf<MealType>()
        val profile = testProfile(
            userId = "user",
            weightKg = 80,
            dailyCalorieTarget = 2000,
            proteinTargetGrams = 125,
            carbTargetGrams = 220,
            fatTargetGrams = 65
        )
        val mealSummaries = listOf(
            mealSummary(MealType.BREAKFAST, calories = 400),
            mealSummary(MealType.LUNCH, calories = 600)
        )
        val exerciseLogs = listOf(exerciseLog(calories = 180, durationMinutes = 45))
        val action = DailySummaryLoadAction(
            loadProfile = { profile },
            getMealSummariesForDate = { date ->
                assertEquals("2026-07-02", date)
                mealSummaries
            },
            getNutritionSummaryForDate = {
                DailyNutritionSummary(
                    totalCalories = 1000,
                    totalProtein = 70.0,
                    totalCarbs = 110.0,
                    totalFat = 35.0
                )
            },
            getExerciseSummaryForDate = {
                ExerciseDailySummary(
                    totalCalories = 180,
                    totalDurationMinutes = 45,
                    logCount = 1
                )
            },
            getExerciseLogsForDate = { exerciseLogs },
            getMealEntryDetailsForDate = { _, mealType ->
                requestedMealTypes += mealType
                listOf(mealDetail(mealType))
            },
            resolveNutritionTargets = {
                DailyNutritionTargetResolver.Targets(
                    dailyCalories = 2000,
                    proteinGrams = 125,
                    carbGrams = 220,
                    fatGrams = 65
                )
            }
        )

        val result = action.load(
            DailySummaryLoadRequest(
                dateIso = "2026-07-02",
                healthConnectSummary = HealthConnectDailySummary(
                    steps = 5000,
                    activeCaloriesKcal = 120,
                    hasPermission = true,
                    isAvailable = true
                ),
                displaySteps = 5000,
                fallbackWeightKg = 70.0
            )
        )

        assertEquals(MealType.entries, requestedMealTypes)
        assertEquals(400, result.mealCalories.breakfast)
        assertEquals(600, result.mealCalories.lunch)
        assertEquals(80.0, result.resolvedWeightKg!!, 0.0)
        assertEquals(80.0, result.displayWeightKg, 0.0)
        assertEquals(1, result.exerciseLogCount)
        assertEquals(exerciseLogs, result.exerciseLogs)
        assertEquals(45, result.summaryState.activityDurationMinutes)
        assertEquals(1000, result.summaryState.energyBalance.eatenCalories)
        assertEquals(300, result.summaryState.energyBalance.burnedCalories)
        assertEquals(1300, result.summaryState.energyBalance.remainingCalories)
        assertEquals(5000, result.summaryState.displaySteps)
    }

    @Test
    fun `missing profile keeps fallback weight and custom meal type request`() = runBlocking {
        val requestedMealTypes = mutableListOf<MealType>()
        val action = DailySummaryLoadAction(
            loadProfile = { null },
            getMealSummariesForDate = { emptyList() },
            getNutritionSummaryForDate = {
                DailyNutritionSummary(0, 0.0, 0.0, 0.0)
            },
            getExerciseSummaryForDate = {
                ExerciseDailySummary(0, 0, 0)
            },
            getExerciseLogsForDate = { emptyList() },
            getMealEntryDetailsForDate = { _, mealType ->
                requestedMealTypes += mealType
                emptyList()
            },
            resolveNutritionTargets = {
                DailyNutritionTargetResolver.Targets(1800, 120, 200, 60)
            }
        )

        val result = action.load(
            DailySummaryLoadRequest(
                dateIso = "2026-07-01",
                healthConnectSummary = HealthConnectDailySummary(),
                displaySteps = 42,
                fallbackWeightKg = 73.5,
                mealTypes = listOf(MealType.DINNER)
            )
        )

        assertNull(result.profile)
        assertNull(result.resolvedWeightKg)
        assertEquals(73.5, result.displayWeightKg, 0.0)
        assertEquals(listOf(MealType.DINNER), requestedMealTypes)
        assertEquals(setOf(MealType.DINNER), result.mealDetailsByType.keys)
        assertEquals(42, result.summaryState.displaySteps)
    }

    private fun mealSummary(
        mealType: MealType,
        calories: Int
    ): DailyMealSummary {
        return DailyMealSummary(
            mealType = mealType.argumentValue,
            totalCalories = calories,
            totalProtein = 10.0,
            totalCarbs = 20.0,
            totalFat = 5.0
        )
    }

    private fun exerciseLog(
        calories: Int,
        durationMinutes: Int
    ): ExerciseLogLocalModel {
        return ExerciseLogLocalModel(
            id = "exercise",
            entryDate = "2026-07-02",
            exerciseId = "walk",
            exerciseTitle = "Walk",
            categoryName = "Cardio",
            durationMinutes = durationMinutes,
            intensityName = "Medium",
            metValue = 4.0,
            calories = calories,
            createdAt = 1L
        )
    }

    private fun mealDetail(mealType: MealType): MealEntryDetailUiModel {
        return MealEntryDetailUiModel(
            entryId = mealType.argumentValue,
            title = mealType.argumentValue,
            sourceType = "manual",
            foodSummary = "Food",
            totalCalories = 100,
            totalProtein = 10.0,
            totalCarbs = 15.0,
            totalFat = 3.0,
            createdAt = 1L,
            foodCount = 1
        )
    }
}
