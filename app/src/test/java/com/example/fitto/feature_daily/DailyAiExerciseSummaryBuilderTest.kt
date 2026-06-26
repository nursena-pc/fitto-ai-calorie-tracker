package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiExerciseSummaryBuilderTest {

    @Test
    fun build_withManualLogs_returnsFormattedManualSummary() {
        val summary = buildSummary(
            logs = listOf(
                exerciseLog(title = "Walk", durationMinutes = 30, calories = 120),
                exerciseLog(title = "Yoga", durationMinutes = 20, calories = 70)
            )
        )

        assertEquals("Walk 30m 120kcal, Yoga 20m 70kcal", summary)
    }

    @Test
    fun build_limitsManualLogsToThreeItems() {
        val summary = buildSummary(
            logs = listOf(
                exerciseLog(title = "One"),
                exerciseLog(title = "Two"),
                exerciseLog(title = "Three"),
                exerciseLog(title = "Four")
            )
        )

        assertEquals("One 10m 50kcal, Two 10m 50kcal, Three 10m 50kcal", summary)
    }

    @Test
    fun build_usesCategoryNameWhenTitleIsBlank() {
        val summary = buildSummary(
            logs = listOf(exerciseLog(title = "", categoryName = "Cardio"))
        )

        assertEquals("Cardio 10m 50kcal", summary)
    }

    @Test
    fun build_withManualAndSyncedActivity_returnsCombinedSummary() {
        val summary = buildSummary(
            logs = listOf(exerciseLog(title = "Walk")),
            syncedActivityCalories = 180
        )

        assertEquals("Walk 10m 50kcal + synced 180kcal", summary)
    }

    @Test
    fun build_withOnlySyncedActivity_returnsSyncedSummary() {
        val summary = buildSummary(
            logs = emptyList(),
            syncedActivityCalories = 180
        )

        assertEquals("synced 180kcal", summary)
    }

    @Test
    fun build_withoutExercise_returnsBlank() {
        val summary = buildSummary(
            logs = emptyList(),
            syncedActivityCalories = 0
        )

        assertEquals("", summary)
    }

    private fun buildSummary(
        logs: List<DailyAiExerciseSummaryBuilder.ExerciseLog>,
        syncedActivityCalories: Int = 0
    ): String {
        return DailyAiExerciseSummaryBuilder.build(
            logs = logs,
            syncedActivityCalories = syncedActivityCalories,
            itemText = { title, duration, calories -> "$title ${duration}m ${calories}kcal" },
            withSyncedText = { manualSummary, syncedCalories ->
                "$manualSummary + synced ${syncedCalories}kcal"
            },
            syncedOnlyText = { syncedCalories -> "synced ${syncedCalories}kcal" }
        )
    }

    private fun exerciseLog(
        title: String,
        categoryName: String = "General",
        durationMinutes: Int = 10,
        calories: Int = 50
    ) = DailyAiExerciseSummaryBuilder.ExerciseLog(
        title = title,
        categoryName = categoryName,
        durationMinutes = durationMinutes,
        calories = calories
    )
}
