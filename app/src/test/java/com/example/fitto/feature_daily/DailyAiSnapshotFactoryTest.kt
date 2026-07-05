package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAiSnapshotFactoryTest {

    @Test
    fun `factory builds localized meal exercise and preference snapshot`() {
        val factory = factory()
        val result = DailyAiTestFixtures.summaryResult(
            profile = DailyAiTestFixtures.profile(
                displayName = "Nursena",
                userName = "nursena",
                goalType = "lose_weight"
            ),
            mealSummariesByType = mapOf(
                MealType.BREAKFAST.argumentValue to DailyMealSummary(
                    mealType = MealType.BREAKFAST.argumentValue,
                    totalCalories = 350,
                    totalProtein = 20.0,
                    totalCarbs = 40.0,
                    totalFat = 10.0
                )
            ),
            mealDetailsByType = mapOf(
                MealType.BREAKFAST to listOf(
                    MealEntryDetailUiModel(
                        entryId = "entry-1",
                        title = "Kahvaltı",
                        sourceType = "manual",
                        foodSummary = "Yumurta, peynir",
                        totalCalories = 360,
                        totalProtein = 22.0,
                        totalCarbs = 38.0,
                        totalFat = 12.0,
                        createdAt = 1L,
                        foodCount = 2
                    )
                )
            ),
            exerciseLogs = listOf(
                ExerciseLogLocalModel(
                    id = "log-1",
                    entryDate = "2026-07-05",
                    exerciseId = "walk",
                    exerciseTitle = "Yürüyüş",
                    categoryName = "cardio",
                    durationMinutes = 30,
                    intensityName = "medium",
                    metValue = 3.5,
                    calories = 140,
                    createdAt = 1L
                )
            ),
            exerciseLogCount = 1
        )

        val snapshot = factory.create(
            request = DailyAiSummarySnapshotRequest(
                dateIso = "2026-07-05",
                summary = result,
                waterMl = 1_000,
                waterGoalMl = 2_500
            ),
            preferences = DailyAiProfilePreferences(
                dietPreferences = "Yüksek protein",
                allergies = "Fındık",
                dislikedFoods = "Kereviz",
                mealRoutine = "3 öğün"
            )
        )

        assertEquals("Nursena", snapshot.userName)
        assertEquals("lose_weight", snapshot.goalType)
        assertEquals(1_000, snapshot.waterMl)
        assertEquals("Yüksek protein", snapshot.dietPreferences)
        assertEquals("Fındık", snapshot.allergies)
        assertEquals("Kereviz", snapshot.dislikedFoods)
        assertEquals("3 öğün", snapshot.mealRoutine)
        assertEquals(1, snapshot.meals.size)
        assertEquals("Breakfast", snapshot.meals.single().mealTypeLabel)
        assertEquals(360, snapshot.meals.single().totalCalories)
        assertTrue(snapshot.meals.single().foodSummary.contains("Yumurta, peynir"))
        assertEquals("Yürüyüş/30/140 + synced 90", snapshot.exerciseSummaryText)
    }

    @Test
    fun `factory falls back to app name when profile names are blank`() {
        val factory = factory()
        val result = DailyAiTestFixtures.summaryResult(
            profile = DailyAiTestFixtures.profile(displayName = "", userName = "")
        )

        val snapshot = factory.create(
            request = DailyAiSummarySnapshotRequest(
                dateIso = "2026-07-05",
                summary = result,
                waterMl = 0,
                waterGoalMl = 2_500
            ),
            preferences = DailyAiProfilePreferences("Standard", "", "", "Flexible")
        )

        assertEquals("Fitto", snapshot.userName)
    }

    @Test
    fun `water update preserves snapshot data and recalculates trigger`() {
        val factory = factory()
        val snapshot = DailyAiSnapshot(
            dateIso = "2026-07-05",
            userName = "User",
            goalType = "maintain",
            dailyCalorieTarget = 1_800,
            totalCalories = 1_700,
            proteinGrams = 120.0,
            proteinTargetGrams = 120,
            carbGrams = 200.0,
            fatGrams = 60.0,
            waterMl = 200,
            waterGoalMl = 2_500,
            triggerType = AiDailyTriggerType.LOW_WATER,
            meals = listOf(
                DailyAiMealSnapshot("Breakfast", 500, 30.0, 50.0, 15.0, 1, "Eggs")
            ),
            exerciseLogCount = 1,
            exerciseDurationMinutes = 30,
            exerciseCalories = 150,
            exerciseSummaryText = "Walk",
            dietPreferences = "Standard",
            allergies = "",
            dislikedFoods = "",
            mealRoutine = "Flexible"
        )

        val updated = factory.updateWater(snapshot, waterMl = 2_500, waterGoalMl = 2_500)

        assertEquals(2_500, updated.waterMl)
        assertEquals(snapshot.userName, updated.userName)
        assertEquals(snapshot.meals, updated.meals)
        assertEquals(
            DailyAiTriggerResolver.resolve(
                totalCalories = snapshot.totalCalories,
                targetCalories = snapshot.dailyCalorieTarget,
                waterMl = 2_500,
                waterGoalMl = 2_500,
                proteinGrams = snapshot.proteinGrams,
                proteinTargetGrams = snapshot.proteinTargetGrams,
                meals = snapshot.meals,
                exerciseLogCount = snapshot.exerciseLogCount,
                snackMealLabel = "Snack"
            ),
            updated.triggerType
        )
    }

    private fun factory(): DailyAiSnapshotFactory {
        return DailyAiSnapshotFactory(
            appName = "Fitto",
            snackMealLabel = "Snack",
            unavailableDetailsText = "Unavailable",
            mealDefinitions = listOf(
                DailyAiMealSnapshotMapper.MealDefinition(
                    key = MealType.BREAKFAST.argumentValue,
                    label = "Breakfast"
                )
            ),
            exerciseItemText = { title, duration, calories -> "$title/$duration/$calories" },
            exerciseWithSyncedText = { manual, synced -> "$manual + synced $synced" },
            exerciseSyncedOnlyText = { synced -> "synced $synced" },
            maxFoodSummaryChars = 360
        )
    }
}

internal object DailyAiTestFixtures {
    fun summaryResult(
        profile: UserProfileEntity? = null,
        mealSummariesByType: Map<String, DailyMealSummary> = emptyMap(),
        mealDetailsByType: Map<MealType, List<MealEntryDetailUiModel>> = emptyMap(),
        exerciseLogs: List<ExerciseLogLocalModel> = emptyList(),
        exerciseLogCount: Int = 0
    ): DailySummaryLoadResult {
        return DailySummaryLoadResult(
            profile = profile,
            nutritionTargets = DailyNutritionTargetResolver.Targets(
                dailyCalories = 1_800,
                proteinGrams = 120,
                carbGrams = 220,
                fatGrams = 60
            ),
            mealSummariesByType = mealSummariesByType,
            mealCalories = DailyMealCaloriesResolver.Calories(
                breakfast = 0,
                lunch = 0,
                dinner = 0,
                snack = 0
            ),
            mealDetailsByType = mealDetailsByType,
            exerciseLogs = exerciseLogs,
            exerciseLogCount = exerciseLogCount,
            resolvedWeightKg = 70.0,
            displayWeightKg = 70.0,
            summaryState = DailySummaryUiStateResolver.State(
                energyBalance = DailyEnergyBalanceCalculator.Result(
                    eatenCalories = 2_000,
                    manualExerciseCalories = 140,
                    stepCalories = 90,
                    syncedActivityCalories = 90,
                    burnedCalories = 230,
                    remainingCalories = 30
                ),
                displaySteps = 4_000,
                activityDurationMinutes = 30,
                calorieProgressPercent = 100,
                carbs = DailySummaryUiStateResolver.MacroState(210.0, 220, 95),
                protein = DailySummaryUiStateResolver.MacroState(80.0, 120, 67),
                fat = DailySummaryUiStateResolver.MacroState(70.0, 60, 100)
            )
        )
    }

    fun profile(
        displayName: String = "User",
        userName: String = "user",
        goalType: String = "maintain"
    ): UserProfileEntity {
        return UserProfileEntity(
            userId = "user-id",
            email = "user@example.com",
            displayName = displayName,
            userName = userName,
            heightCm = 170,
            weightKg = 70,
            targetWeightKg = 65,
            age = 30,
            gender = "female",
            goalType = goalType,
            activityLevel = "moderate",
            bmr = 1_500,
            maintenanceCalories = 2_000,
            targetCalories = 1_800,
            recommendedCalories = 1_800,
            dailyCalorieTarget = 1_800,
            proteinTargetGrams = 120,
            carbTargetGrams = 220,
            fatTargetGrams = 60,
            waterTargetMl = 2_500,
            weeklyWeightChangeKg = -0.5,
            estimatedWeeks = 10,
            planStatus = "HEALTHY",
            profileCompleted = true,
            updatedAt = 1L,
            syncedAt = 1L
        )
    }
}
