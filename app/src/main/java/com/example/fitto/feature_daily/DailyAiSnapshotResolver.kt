package com.example.fitto.feature_daily

object DailyAiSnapshotResolver {

    fun resolve(
        dateIso: String,
        userName: String,
        goalType: String,
        nutritionTargets: DailyNutritionTargetResolver.Targets,
        summaryState: DailySummaryUiStateResolver.State,
        waterMl: Int,
        waterGoalMl: Int,
        meals: List<DailyAiMealSnapshot>,
        exerciseLogCount: Int,
        exerciseSummaryText: String,
        dietPreferences: String,
        allergies: String,
        dislikedFoods: String,
        mealRoutine: String,
        snackMealLabel: String
    ): DailyAiSnapshot {
        val triggerType = DailyAiTriggerResolver.resolve(
            totalCalories = summaryState.energyBalance.eatenCalories,
            targetCalories = nutritionTargets.dailyCalories,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            proteinGrams = summaryState.protein.currentGrams,
            proteinTargetGrams = summaryState.protein.targetGrams,
            meals = meals,
            exerciseLogCount = exerciseLogCount,
            snackMealLabel = snackMealLabel
        )

        return DailyAiSnapshot(
            dateIso = dateIso,
            userName = userName,
            goalType = goalType,
            dailyCalorieTarget = nutritionTargets.dailyCalories,
            totalCalories = summaryState.energyBalance.eatenCalories,
            proteinGrams = summaryState.protein.currentGrams,
            proteinTargetGrams = summaryState.protein.targetGrams,
            carbGrams = summaryState.carbs.currentGrams,
            fatGrams = summaryState.fat.currentGrams,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            triggerType = triggerType,
            meals = meals,
            exerciseLogCount = exerciseLogCount,
            exerciseDurationMinutes = summaryState.activityDurationMinutes,
            exerciseCalories = summaryState.energyBalance.burnedCalories,
            exerciseSummaryText = exerciseSummaryText,
            dietPreferences = dietPreferences,
            allergies = allergies,
            dislikedFoods = dislikedFoods,
            mealRoutine = mealRoutine
        )
    }
}
