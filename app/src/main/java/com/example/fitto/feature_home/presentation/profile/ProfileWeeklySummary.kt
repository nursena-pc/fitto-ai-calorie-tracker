package com.example.fitto.feature_home.presentation.profile

import kotlin.math.roundToInt

data class ProfileWeeklySummary(
    val todayCalories: Int = 0,
    val todayProtein: Double = 0.0,
    val todayCarbs: Double = 0.0,
    val todayFat: Double = 0.0,
    val weeklyTotalCalories: Int = 0,
    val weeklyAverageCalories: Int = 0,
    val recordedDays: Int = 0,
    val weeklyProtein: Double = 0.0,
    val weeklyCarbs: Double = 0.0,
    val weeklyFat: Double = 0.0
) {
    val hasNutritionData: Boolean
        get() = todayCalories > 0 ||
                todayProtein > 0.0 ||
                todayCarbs > 0.0 ||
                todayFat > 0.0 ||
                weeklyTotalCalories > 0 ||
                recordedDays > 0

    val todayMacroTotal: Double
        get() = todayProtein + todayCarbs + todayFat

    val weeklyMacroTotal: Double
        get() = weeklyProtein + weeklyCarbs + weeklyFat

    val proteinSharePercent: Float
        get() = macroShare(weeklyProtein)

    val carbSharePercent: Float
        get() = macroShare(weeklyCarbs)

    val fatSharePercent: Float
        get() = macroShare(weeklyFat)

    fun calorieProgressPercent(targetCalories: Int): Int {
        if (targetCalories <= 0) return 0
        return ((todayCalories.toDouble() / targetCalories.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun macroProgressPercent(
        proteinGoal: Int,
        carbGoal: Int,
        fatGoal: Int
    ): Int {
        val targetTotal = proteinGoal + carbGoal + fatGoal

        if (targetTotal <= 0) return 0

        return ((todayMacroTotal / targetTotal.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun macroShare(value: Double): Float {
        if (weeklyMacroTotal <= 0.0) return 0f
        return ((value / weeklyMacroTotal) * 100.0).toFloat()
    }

    companion object {
        val Empty = ProfileWeeklySummary()
    }
}
