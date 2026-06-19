package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import kotlin.math.roundToInt

object DailyAiTriggerPolicy {

    data class Meal(
        val mealTypeLabel: String,
        val totalCalories: Int,
        val proteinGrams: Double
    )

    fun resolve(
        totalCalories: Int,
        targetCalories: Int,
        waterMl: Int,
        waterGoalMl: Int,
        proteinGrams: Double,
        proteinTargetGrams: Int,
        meals: List<Meal>,
        exerciseLogCount: Int,
        snackMealLabel: String
    ): AiDailyTriggerType {
        if (meals.isEmpty()) return AiDailyTriggerType.NO_MEAL

        if (targetCalories > 0) {
            val redThreshold = (targetCalories * 1.2).roundToInt()
            val yellowThreshold = (targetCalories * 0.9).roundToInt()
            if (totalCalories >= redThreshold) return AiDailyTriggerType.CALORIE_EXCEEDED
            if (totalCalories >= yellowThreshold) return AiDailyTriggerType.CALORIE_REACHED
        }

        val mainMeals = meals.filterNot { meal ->
            meal.mealTypeLabel.equals(snackMealLabel, ignoreCase = true)
        }

        if (mainMeals.any { it.totalCalories in 1..180 }) {
            return AiDailyTriggerType.INSUFFICIENT_MEAL
        }

        if (mainMeals.any { it.proteinGrams < 8.0 && it.totalCalories > 0 }) {
            return AiDailyTriggerType.LOW_MEAL_PROTEIN
        }

        if (waterGoalMl > 0 && waterMl < (waterGoalMl * 0.5).roundToInt()) {
            return AiDailyTriggerType.LOW_WATER
        }

        if (proteinTargetGrams > 0 && proteinGrams < proteinTargetGrams * 0.55) {
            return AiDailyTriggerType.LOW_DAILY_PROTEIN
        }

        if (exerciseLogCount == 0) {
            return AiDailyTriggerType.NO_EXERCISE
        }

        return AiDailyTriggerType.NORMAL
    }
}
