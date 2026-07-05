package com.example.fitto.feature_daily

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel

internal data class DailyAiSummarySnapshotRequest(
    val dateIso: String,
    val summary: DailySummaryLoadResult,
    val waterMl: Int,
    val waterGoalMl: Int
)

internal data class DailyAiProfilePreferences(
    val dietPreferences: String,
    val allergies: String,
    val dislikedFoods: String,
    val mealRoutine: String
)

internal interface DailyAiSnapshotBuilder {
    fun create(
        request: DailyAiSummarySnapshotRequest,
        preferences: DailyAiProfilePreferences
    ): DailyAiSnapshot

    fun updateWater(
        snapshot: DailyAiSnapshot,
        waterMl: Int,
        waterGoalMl: Int
    ): DailyAiSnapshot
}

internal class DailyAiSnapshotFactory(
    private val appName: String,
    private val snackMealLabel: String,
    private val unavailableDetailsText: String,
    private val mealDefinitions: List<DailyAiMealSnapshotMapper.MealDefinition>,
    private val exerciseItemText: (title: String, durationMinutes: Int, calories: Int) -> String,
    private val exerciseWithSyncedText: (manualSummary: String, syncedCalories: Int) -> String,
    private val exerciseSyncedOnlyText: (syncedCalories: Int) -> String,
    private val maxFoodSummaryChars: Int
) : DailyAiSnapshotBuilder {

    override fun create(
        request: DailyAiSummarySnapshotRequest,
        preferences: DailyAiProfilePreferences
    ): DailyAiSnapshot {
        val result = request.summary
        val profile = result.profile
        val meals = DailyAiMealSnapshotMapper.buildMeals(
            mealDefinitions = mealDefinitions,
            mealSummariesByType = result.mealSummariesByType,
            mealDetailsByType = result.mealDetailsByType.mapKeys { (mealType, _) ->
                mealType.argumentValue
            },
            unavailableDetailsText = unavailableDetailsText,
            maxFoodSummaryChars = maxFoodSummaryChars
        )
        val syncedActivityCalories = result.summaryState.energyBalance.syncedActivityCalories

        return DailyAiSnapshotResolver.resolve(
            dateIso = request.dateIso,
            userName = DailyAiUserNameResolver.resolve(
                displayName = profile?.displayName,
                userName = profile?.userName,
                fallbackName = appName
            ),
            goalType = profile?.goalType.orEmpty(),
            nutritionTargets = result.nutritionTargets,
            summaryState = result.summaryState,
            waterMl = request.waterMl,
            waterGoalMl = request.waterGoalMl,
            meals = meals,
            exerciseLogCount = result.exerciseLogCount,
            exerciseSummaryText = buildExerciseSummary(
                logs = result.exerciseLogs,
                syncedActivityCalories = syncedActivityCalories
            ),
            dietPreferences = preferences.dietPreferences,
            allergies = preferences.allergies,
            dislikedFoods = preferences.dislikedFoods,
            mealRoutine = preferences.mealRoutine,
            snackMealLabel = snackMealLabel
        )
    }

    override fun updateWater(
        snapshot: DailyAiSnapshot,
        waterMl: Int,
        waterGoalMl: Int
    ): DailyAiSnapshot {
        val updatedTrigger = DailyAiTriggerResolver.resolve(
            totalCalories = snapshot.totalCalories,
            targetCalories = snapshot.dailyCalorieTarget,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            proteinGrams = snapshot.proteinGrams,
            proteinTargetGrams = snapshot.proteinTargetGrams,
            meals = snapshot.meals,
            exerciseLogCount = snapshot.exerciseLogCount,
            snackMealLabel = snackMealLabel
        )

        return snapshot.copy(
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            triggerType = updatedTrigger
        )
    }

    private fun buildExerciseSummary(
        logs: List<ExerciseLogLocalModel>,
        syncedActivityCalories: Int
    ): String {
        return DailyAiExerciseSummaryBuilder.build(
            logs = DailyAiExerciseLogMapper.toExerciseLogs(logs),
            syncedActivityCalories = syncedActivityCalories,
            itemText = exerciseItemText,
            withSyncedText = exerciseWithSyncedText,
            syncedOnlyText = exerciseSyncedOnlyText
        )
    }
}
