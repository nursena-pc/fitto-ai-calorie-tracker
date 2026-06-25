package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import kotlin.math.roundToInt

object ExerciseCalorieCalculator {

    fun calculate(
        activity: ExerciseUiModel,
        minutes: Int,
        intensity: ExerciseIntensity,
        userWeightKg: Double
    ): Int {
        val durationHours = minutes / 60.0

        return (activity.metValue * userWeightKg * durationHours * intensity.multiplier)
            .roundToInt()
            .coerceAtLeast(0)
    }
}
