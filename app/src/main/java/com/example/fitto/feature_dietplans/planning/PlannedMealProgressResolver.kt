package com.example.fitto.feature_dietplans.planning

import kotlin.math.roundToInt

object PlannedMealProgressResolver {

    fun nextMeal(
        meals: List<PlannedMealUiModel>,
        completedMealIds: Set<String>
    ): PlannedMealUiModel? {
        return meals.firstOrNull { meal -> meal.id !in completedMealIds }
    }

    fun progressPercent(
        meals: List<PlannedMealUiModel>,
        completedMealIds: Set<String>
    ): Int {
        return if (meals.isEmpty()) {
            0
        } else {
            ((completedMealIds.size.toFloat() / meals.size.toFloat()) * 100).roundToInt()
        }
    }

    fun isCompleted(
        meals: List<PlannedMealUiModel>,
        completedMealIds: Set<String>
    ): Boolean {
        return meals.isNotEmpty() && completedMealIds.size >= meals.size
    }
}
