package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseDisplayTextResolverTest {

    @Test
    fun resolveTitle_returnsLocalizedKnownActivityTitle() {
        assertEquals(
            "Koşu • Canlı tempo",
            ExerciseDisplayTextResolver.resolveTitle(activity(title = "Running", metValue = 6.0))
        )
    }

    @Test
    fun resolveTitle_trimsGenericCatalogTitle() {
        assertEquals(
            "Custom workout",
            ExerciseDisplayTextResolver.resolveTitle(
                activity(title = "Custom workout, extra catalog detail", metValue = 3.0)
            )
        )
    }

    @Test
    fun resolveDescription_combinesCategoryAndIntensity() {
        assertEquals(
            "Kardiyo • Yoğun efor",
            ExerciseDisplayTextResolver.resolveDescription(
                activity(title = "Cycling", metValue = 8.0),
                categoryName = "Kardiyo"
            )
        )
    }

    private fun activity(
        title: String,
        metValue: Double
    ): ExerciseUiModel {
        return ExerciseUiModel(
            id = title,
            title = title,
            description = "",
            category = ExerciseCategory.CARDIO,
            metValue = metValue,
            defaultDurationMinutes = 30,
            iconEmoji = ""
        )
    }
}
