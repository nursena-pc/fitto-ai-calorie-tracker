package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.feature_exercise.data.ExerciseDailySummary

object DailySummaryUiStateResolver {

    data class State(
        val energyBalance: DailyEnergyBalanceCalculator.Result,
        val displaySteps: Long,
        val activityDurationMinutes: Int,
        val calorieProgressPercent: Int,
        val carbs: MacroState,
        val protein: MacroState,
        val fat: MacroState
    )

    data class MacroState(
        val currentGrams: Double,
        val targetGrams: Int,
        val progressPercent: Int
    )

    fun resolve(
        nutritionSummary: DailyNutritionSummary,
        nutritionTargets: DailyNutritionTargetResolver.Targets,
        exerciseSummary: ExerciseDailySummary,
        healthActiveCalories: Int,
        displaySteps: Long,
        weightKg: Double
    ): State {
        val energyBalance = DailyEnergyBalanceCalculator.calculate(
            targetCalories = nutritionTargets.dailyCalories,
            eatenCalories = nutritionSummary.totalCalories,
            manualExerciseCalories = exerciseSummary.totalCalories,
            healthActiveCalories = healthActiveCalories,
            displaySteps = displaySteps,
            weightKg = weightKg
        )

        return State(
            energyBalance = energyBalance,
            displaySteps = displaySteps,
            activityDurationMinutes = exerciseSummary.totalDurationMinutes.coerceAtLeast(0),
            calorieProgressPercent = DailyMetricCalculator.progressPercent(
                current = energyBalance.eatenCalories.toDouble(),
                target = nutritionTargets.dailyCalories.toDouble()
            ),
            carbs = macroState(
                currentGrams = nutritionSummary.totalCarbs,
                targetGrams = nutritionTargets.carbGrams
            ),
            protein = macroState(
                currentGrams = nutritionSummary.totalProtein,
                targetGrams = nutritionTargets.proteinGrams
            ),
            fat = macroState(
                currentGrams = nutritionSummary.totalFat,
                targetGrams = nutritionTargets.fatGrams
            )
        )
    }

    private fun macroState(
        currentGrams: Double,
        targetGrams: Int
    ): MacroState {
        return MacroState(
            currentGrams = currentGrams,
            targetGrams = targetGrams,
            progressPercent = DailyMetricCalculator.progressPercent(
                current = currentGrams,
                target = targetGrams.toDouble()
            )
        )
    }
}
