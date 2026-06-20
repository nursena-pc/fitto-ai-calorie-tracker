package com.example.fitto.feature_daily

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel

object DailyAiExerciseLogMapper {

    fun toExerciseLogs(
        logs: List<ExerciseLogLocalModel>
    ): List<DailyAiExerciseSummaryBuilder.ExerciseLog> {
        return logs.map { log ->
            DailyAiExerciseSummaryBuilder.ExerciseLog(
                title = log.exerciseTitle,
                categoryName = log.categoryName,
                durationMinutes = log.durationMinutes,
                calories = log.calories
            )
        }
    }
}
