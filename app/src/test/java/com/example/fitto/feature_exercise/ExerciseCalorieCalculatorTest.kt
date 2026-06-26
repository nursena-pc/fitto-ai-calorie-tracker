package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCalorieCalculatorTest {

    @Test
    fun calculate_usesMetWeightDurationAndIntensity() {
        val calories = ExerciseCalorieCalculator.calculate(
            activity = activity(metValue = 6.0),
            minutes = 30,
            intensity = ExerciseIntensity.VIGOROUS,
            userWeightKg = 70.0
        )

        assertEquals(263, calories)
    }

    @Test
    fun calculate_clampsNegativeResult() {
        val calories = ExerciseCalorieCalculator.calculate(
            activity = activity(metValue = 6.0),
            minutes = -30,
            intensity = ExerciseIntensity.MODERATE,
            userWeightKg = 70.0
        )

        assertEquals(0, calories)
    }

    private fun activity(metValue: Double): ExerciseUiModel {
        return ExerciseUiModel(
            id = "run",
            title = "Running",
            description = "",
            category = ExerciseCategory.CARDIO,
            metValue = metValue,
            defaultDurationMinutes = 30,
            iconEmoji = ""
        )
    }
}
