package com.example.fitto.feature_exercise.model

import androidx.annotation.StringRes
import com.example.fitto.R

enum class ExerciseIntensity(
    @StringRes val titleRes: Int,
    val multiplier: Double
) {
    LIGHT(
        titleRes = R.string.exercise_intensity_light,
        multiplier = 0.85
    ),
    MODERATE(
        titleRes = R.string.exercise_intensity_moderate,
        multiplier = 1.0
    ),
    VIGOROUS(
        titleRes = R.string.exercise_intensity_vigorous,
        multiplier = 1.25
    );

    fun next(): ExerciseIntensity {
        return when (this) {
            LIGHT -> MODERATE
            MODERATE -> VIGOROUS
            VIGOROUS -> LIGHT
        }
    }
}