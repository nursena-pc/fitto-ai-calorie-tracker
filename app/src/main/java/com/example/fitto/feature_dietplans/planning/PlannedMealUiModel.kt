package com.example.fitto.feature_dietplans.planning

import com.example.fitto.feature_meal.model.MealType
import kotlin.math.roundToInt

data class PlannedMealUiModel(
    val id: String,
    val planId: String,
    val dayNumber: Int,
    val mealType: MealType,
    val title: String,
    val baseCalories: Int,
    val foods: List<String>
) {
    fun scaledCalories(targetCalories: Int): Int {
        val safeTargetCalories = targetCalories.coerceIn(MIN_TARGET_CALORIES, MAX_TARGET_CALORIES)
        val scale = safeTargetCalories.toDouble() / BASELINE_CALORIES.toDouble()
        return (baseCalories * scale).roundToInt().roundToNearest(5).coerceAtLeast(60)
    }

    fun scaledFoodSummary(targetCalories: Int): String {
        val safeTargetCalories = targetCalories.coerceIn(MIN_TARGET_CALORIES, MAX_TARGET_CALORIES)
        val scale = safeTargetCalories.toDouble() / BASELINE_CALORIES.toDouble()
        val scaleLabel = when {
            scale < 0.88 -> " · küçük porsiyon"
            scale > 1.12 -> " · artırılmış porsiyon"
            else -> ""
        }
        return foods.joinToString(separator = ", ") + scaleLabel
    }

    companion object {
        const val BASELINE_CALORIES = 1800
        const val MIN_TARGET_CALORIES = 1200
        const val MAX_TARGET_CALORIES = 3200
    }
}

data class DailyPlannedMealsUiModel(
    val planId: String,
    val planTitle: String,
    val planBadge: String,
    val planIcon: String,
    val dayNumber: Int,
    val durationDays: Int,
    val targetCalories: Int,
    val meals: List<PlannedMealUiModel>,
    val nextMeal: PlannedMealUiModel?,
    val completedMealIds: Set<String>
) {
    val progressPercent: Int
        get() = if (meals.isEmpty()) 0 else ((completedMealIds.size.toFloat() / meals.size.toFloat()) * 100).roundToInt()

    val isCompleted: Boolean
        get() = meals.isNotEmpty() && completedMealIds.size >= meals.size
}

private fun Int.roundToNearest(step: Int): Int {
    return ((this + step / 2) / step) * step
}
