package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyTriggerType

object DailyAiTriggerResolver {

    fun resolve(
        totalCalories: Int,
        targetCalories: Int,
        waterMl: Int,
        waterGoalMl: Int,
        proteinGrams: Double,
        proteinTargetGrams: Int,
        meals: List<DailyAiMealSnapshot>,
        exerciseLogCount: Int,
        snackMealLabel: String
    ): AiDailyTriggerType {
        return DailyAiTriggerPolicy.resolve(
            totalCalories = totalCalories,
            targetCalories = targetCalories,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            proteinGrams = proteinGrams,
            proteinTargetGrams = proteinTargetGrams,
            meals = meals.map { meal ->
                DailyAiTriggerPolicy.Meal(
                    mealTypeLabel = meal.mealTypeLabel,
                    totalCalories = meal.totalCalories,
                    proteinGrams = meal.proteinGrams
                )
            },
            exerciseLogCount = exerciseLogCount,
            snackMealLabel = snackMealLabel
        )
    }
}
