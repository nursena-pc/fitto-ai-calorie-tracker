package com.example.fitto.feature_daily

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiExerciseLogMapperTest {

    @Test
    fun toExerciseLogs_mapsLocalModelsToAiSummaryLogs() {
        val logs = DailyAiExerciseLogMapper.toExerciseLogs(
            listOf(
                ExerciseLogLocalModel(
                    id = "1",
                    entryDate = "2026-06-20",
                    exerciseId = "walk",
                    exerciseTitle = "Walk",
                    categoryName = "Cardio",
                    durationMinutes = 30,
                    intensityName = "LOW",
                    metValue = 3.0,
                    calories = 120,
                    createdAt = 100L
                )
            )
        )

        assertEquals(
            listOf(
                DailyAiExerciseSummaryBuilder.ExerciseLog(
                    title = "Walk",
                    categoryName = "Cardio",
                    durationMinutes = 30,
                    calories = 120
                )
            ),
            logs
        )
    }
}
