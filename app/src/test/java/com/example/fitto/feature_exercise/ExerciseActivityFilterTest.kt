package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseDurationRange
import com.example.fitto.feature_exercise.model.ExerciseFilterState
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseSort
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseActivityFilterTest {

    @Test
    fun resolveVisibleActivities_withoutSearch_filtersByCategoryAndDuration() {
        val activities = listOf(
            activity(id = "walk", category = ExerciseCategory.CARDIO, duration = 15),
            activity(id = "run", category = ExerciseCategory.CARDIO, duration = 35),
            activity(id = "yoga", category = ExerciseCategory.STRETCHING, duration = 15)
        )

        val result = ExerciseActivityFilter.resolveVisibleActivities(
            activities = activities,
            searchableTextById = ExerciseActivitySearchCache.build(activities),
            selectedCategory = ExerciseCategory.CARDIO,
            filterState = ExerciseFilterState(durationRange = ExerciseDurationRange.SHORT),
            selectedIntensity = ExerciseIntensity.MODERATE,
            rawQuery = "",
            userWeightKg = 70.0
        )

        assertEquals(listOf("walk"), result.map { it.id })
    }

    @Test
    fun resolveVisibleActivities_withSearchIgnoresCategoryAndDurationFilters() {
        val activities = listOf(
            activity(id = "run", title = "Running", category = ExerciseCategory.CARDIO, duration = 35),
            activity(id = "weights", title = "Strength", category = ExerciseCategory.STRENGTH, duration = 15)
        )

        val result = ExerciseActivityFilter.resolveVisibleActivities(
            activities = activities,
            searchableTextById = ExerciseActivitySearchCache.build(activities),
            selectedCategory = ExerciseCategory.STRENGTH,
            filterState = ExerciseFilterState(durationRange = ExerciseDurationRange.SHORT),
            selectedIntensity = ExerciseIntensity.MODERATE,
            rawQuery = "koşu",
            userWeightKg = 70.0
        )

        assertEquals(listOf("run"), result.map { it.id })
    }

    @Test
    fun resolveVisibleActivities_sortsByCalculatedCaloriesDescending() {
        val activities = listOf(
            activity(id = "easy", metValue = 2.0, duration = 20),
            activity(id = "hard", metValue = 8.0, duration = 30),
            activity(id = "medium", metValue = 4.0, duration = 30)
        )

        val result = ExerciseActivityFilter.resolveVisibleActivities(
            activities = activities,
            searchableTextById = ExerciseActivitySearchCache.build(activities),
            selectedCategory = ExerciseCategory.CARDIO,
            filterState = ExerciseFilterState(sort = ExerciseSort.KCAL_HIGH_LOW),
            selectedIntensity = ExerciseIntensity.MODERATE,
            rawQuery = "",
            userWeightKg = 70.0
        )

        assertEquals(listOf("hard", "medium", "easy"), result.map { it.id })
    }

    private fun activity(
        id: String,
        title: String = id,
        category: ExerciseCategory = ExerciseCategory.CARDIO,
        duration: Int,
        metValue: Double = 4.0
    ): ExerciseUiModel {
        return ExerciseUiModel(
            id = id,
            title = title,
            description = title,
            category = category,
            metValue = metValue,
            defaultDurationMinutes = duration,
            iconEmoji = "",
            searchKeywords = listOf(title)
        )
    }
}
