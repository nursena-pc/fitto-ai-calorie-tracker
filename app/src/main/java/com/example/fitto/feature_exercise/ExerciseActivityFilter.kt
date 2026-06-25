package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseFilterState
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseSort
import com.example.fitto.feature_exercise.model.ExerciseUiModel

object ExerciseActivityFilter {

    fun resolveVisibleActivities(
        activities: List<ExerciseUiModel>,
        searchableTextById: Map<String, String>,
        selectedCategory: ExerciseCategory,
        filterState: ExerciseFilterState,
        selectedIntensity: ExerciseIntensity,
        rawQuery: String,
        userWeightKg: Double
    ): List<ExerciseUiModel> {
        val normalizedQuery = ExerciseSearchTextNormalizer.normalize(rawQuery.trim())
        val queryWords = ExerciseSearchQueryResolver.resolve(normalizedQuery)
        val isSearching = queryWords.isNotEmpty()
        val listIntensity = filterState.intensity ?: selectedIntensity

        val filtered = activities.filter { activity ->
            val searchableText = searchableTextById[activity.id].orEmpty()
            val categoryMatches = isSearching || activity.category == selectedCategory
            val durationMatches = isSearching ||
                filterState.durationRange.matches(activity.defaultDurationMinutes)
            val searchMatches = !isSearching || queryWords.any { query ->
                searchableText.contains(query)
            }

            categoryMatches && durationMatches && searchMatches
        }

        return when (filterState.sort) {
            ExerciseSort.RECOMMENDED -> filtered
            ExerciseSort.KCAL_LOW_HIGH -> filtered.sortedBy { activity ->
                ExerciseCalorieCalculator.calculate(
                    activity = activity,
                    minutes = activity.defaultDurationMinutes,
                    intensity = listIntensity,
                    userWeightKg = userWeightKg
                )
            }
            ExerciseSort.KCAL_HIGH_LOW -> filtered.sortedByDescending { activity ->
                ExerciseCalorieCalculator.calculate(
                    activity = activity,
                    minutes = activity.defaultDurationMinutes,
                    intensity = listIntensity,
                    userWeightKg = userWeightKg
                )
            }
            ExerciseSort.DURATION_LOW_HIGH -> filtered.sortedBy { activity ->
                activity.defaultDurationMinutes
            }
        }
    }
}
