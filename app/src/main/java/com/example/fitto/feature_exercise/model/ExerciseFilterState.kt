package com.example.fitto.feature_exercise.model

import androidx.annotation.StringRes
import com.example.fitto.R

data class ExerciseFilterState(
    val durationRange: ExerciseDurationRange = ExerciseDurationRange.ALL,
    val intensity: ExerciseIntensity? = null,
    val sort: ExerciseSort = ExerciseSort.RECOMMENDED
)

enum class ExerciseDurationRange(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int?,
    val minMinutes: Int?,
    val maxMinutes: Int?
) {
    ALL(
        titleRes = R.string.exercise_filter_all,
        descRes = null,
        minMinutes = null,
        maxMinutes = null
    ),
    SHORT(
        titleRes = R.string.exercise_filter_duration_short,
        descRes = R.string.exercise_filter_duration_short_desc,
        minMinutes = 0,
        maxMinutes = 20
    ),
    MEDIUM(
        titleRes = R.string.exercise_filter_duration_medium,
        descRes = R.string.exercise_filter_duration_medium_desc,
        minMinutes = 21,
        maxMinutes = 45
    ),
    LONG(
        titleRes = R.string.exercise_filter_duration_long,
        descRes = R.string.exercise_filter_duration_long_desc,
        minMinutes = 46,
        maxMinutes = null
    );

    fun matches(minutes: Int): Boolean {
        val minOk = minMinutes?.let { minutes >= it } ?: true
        val maxOk = maxMinutes?.let { minutes <= it } ?: true
        return minOk && maxOk
    }
}

enum class ExerciseSort(
    @StringRes val titleRes: Int
) {
    RECOMMENDED(R.string.exercise_sort_recommended),
    KCAL_LOW_HIGH(R.string.exercise_sort_kcal_low_high),
    KCAL_HIGH_LOW(R.string.exercise_sort_kcal_high_low),
    DURATION_LOW_HIGH(R.string.exercise_sort_duration_low_high)
}