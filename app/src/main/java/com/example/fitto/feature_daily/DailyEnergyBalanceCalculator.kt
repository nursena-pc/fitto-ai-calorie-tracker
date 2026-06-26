package com.example.fitto.feature_daily

object DailyEnergyBalanceCalculator {

    data class Result(
        val eatenCalories: Int,
        val manualExerciseCalories: Int,
        val stepCalories: Int,
        val syncedActivityCalories: Int,
        val burnedCalories: Int,
        val remainingCalories: Int
    )

    fun calculate(
        targetCalories: Int,
        eatenCalories: Int,
        manualExerciseCalories: Int,
        healthActiveCalories: Int,
        displaySteps: Long,
        weightKg: Double
    ): Result {
        val safeEatenCalories = eatenCalories.coerceAtLeast(0)
        val safeManualExerciseCalories = manualExerciseCalories.coerceAtLeast(0)
        val safeHealthActiveCalories = healthActiveCalories.coerceAtLeast(0)
        val stepCalories = if (safeHealthActiveCalories > 0) {
            0
        } else {
            DailyMetricCalculator.calculateStepCalories(
                steps = displaySteps.coerceAtLeast(0L),
                weightKg = weightKg
            )
        }
        val syncedActivityCalories = maxOf(safeHealthActiveCalories, stepCalories)
        val burnedCalories = safeManualExerciseCalories + syncedActivityCalories
        val remainingCalories = (targetCalories - safeEatenCalories + burnedCalories)
            .coerceAtLeast(0)

        return Result(
            eatenCalories = safeEatenCalories,
            manualExerciseCalories = safeManualExerciseCalories,
            stepCalories = stepCalories,
            syncedActivityCalories = syncedActivityCalories,
            burnedCalories = burnedCalories,
            remainingCalories = remainingCalories
        )
    }
}
