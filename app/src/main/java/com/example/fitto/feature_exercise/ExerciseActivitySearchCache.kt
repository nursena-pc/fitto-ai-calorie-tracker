package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseUiModel

object ExerciseActivitySearchCache {

    fun build(activities: List<ExerciseUiModel>): Map<String, String> {
        return activities.associate { activity ->
            activity.id to ExerciseSearchTextNormalizer.normalize(
                listOf(
                    activity.id,
                    activity.title,
                    activity.description,
                    activity.searchKeywords.joinToString(" "),
                    activity.sourceDescription,
                    activity.sourceMajorHeading
                ).joinToString(" ")
            )
        }
    }
}
