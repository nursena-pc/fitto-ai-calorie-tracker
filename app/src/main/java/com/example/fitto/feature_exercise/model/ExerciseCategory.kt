package com.example.fitto.feature_exercise.model

import androidx.annotation.StringRes
import com.example.fitto.R

enum class ExerciseCategory(
    @StringRes val titleRes: Int
) {
    CARDIO(R.string.exercise_category_cardio),
    STRENGTH(R.string.exercise_category_strength),
    STRETCHING(R.string.exercise_category_stretching),
    LIFESTYLE(R.string.exercise_category_lifestyle)
}