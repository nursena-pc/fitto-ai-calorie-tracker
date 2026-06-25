package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseLegacyActivityResolverTest {

    @Test
    fun resolveActivityForLog_matchesCatalogActivityByNormalizedTitle() {
        val running = activity(id = "catalog-run", title = "Kosu")

        val resolved = ExerciseLegacyActivityResolver.resolveActivityForLog(
            log = log(exerciseTitle = "Koşu"),
            activities = listOf(running),
            fallbackTitle = "Add Exercise",
            fallbackCategoryName = "Cardio",
            estimatedCaloriesText = "Estimated kcal"
        )

        assertEquals(running, resolved)
    }

    @Test
    fun resolveActivityForLog_createsLegacyActivityWhenCatalogDoesNotMatch() {
        val resolved = ExerciseLegacyActivityResolver.resolveActivityForLog(
            log = log(
                exerciseId = "",
                exerciseTitle = "",
                categoryName = "Kuvvet",
                durationMinutes = 999,
                metValue = -1.0
            ),
            activities = emptyList(),
            fallbackTitle = "Add Exercise",
            fallbackCategoryName = "Cardio",
            estimatedCaloriesText = "Estimated kcal",
            currentTimeMillis = { 123L }
        )

        assertEquals("legacy_123", resolved.id)
        assertEquals("Add Exercise", resolved.title)
        assertEquals("Kuvvet · Estimated kcal", resolved.description)
        assertEquals(ExerciseCategory.STRENGTH, resolved.category)
        assertEquals(4.3, resolved.metValue, 0.0)
        assertEquals(240, resolved.defaultDurationMinutes)
        assertEquals("🔥", resolved.iconEmoji)
    }

    @Test
    fun resolveEmoji_prefersCatalogEmojiAndFallsBackToLegacyEmoji() {
        val activities = listOf(activity(id = "catalog-run", iconEmoji = "R"))

        assertEquals(
            "R",
            ExerciseLegacyActivityResolver.resolveEmoji("catalog-run", activities)
        )
        assertEquals(
            "🏃",
            ExerciseLegacyActivityResolver.resolveEmoji("running_legacy", activities)
        )
    }

    private fun activity(
        id: String,
        title: String = "Running",
        iconEmoji: String = ""
    ): ExerciseUiModel {
        return ExerciseUiModel(
            id = id,
            title = title,
            description = "",
            category = ExerciseCategory.CARDIO,
            metValue = 6.0,
            defaultDurationMinutes = 30,
            iconEmoji = iconEmoji
        )
    }

    private fun log(
        exerciseId: String = "unknown",
        exerciseTitle: String = "Unknown",
        categoryName: String = "Cardio",
        durationMinutes: Int = 30,
        metValue: Double = 6.0
    ): ExerciseLogLocalModel {
        return ExerciseLogLocalModel(
            id = "log-1",
            entryDate = "2026-06-25",
            exerciseId = exerciseId,
            exerciseTitle = exerciseTitle,
            categoryName = categoryName,
            durationMinutes = durationMinutes,
            intensityName = "Moderate",
            metValue = metValue,
            calories = 100,
            createdAt = 0L
        )
    }
}
