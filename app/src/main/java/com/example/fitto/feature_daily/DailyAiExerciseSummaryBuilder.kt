package com.example.fitto.feature_daily

object DailyAiExerciseSummaryBuilder {

    data class ExerciseLog(
        val title: String,
        val categoryName: String,
        val durationMinutes: Int,
        val calories: Int
    )

    fun build(
        logs: List<ExerciseLog>,
        syncedActivityCalories: Int,
        itemText: (String, Int, Int) -> String,
        withSyncedText: (String, Int) -> String,
        syncedOnlyText: (Int) -> String
    ): String {
        val manualSummary = logs
            .take(MAX_MANUAL_LOGS)
            .joinToString(separator = ", ") { log ->
                itemText(
                    log.title.ifBlank { log.categoryName },
                    log.durationMinutes,
                    log.calories
                )
            }

        return when {
            manualSummary.isNotBlank() && syncedActivityCalories > 0 -> {
                withSyncedText(manualSummary, syncedActivityCalories)
            }
            manualSummary.isNotBlank() -> manualSummary
            syncedActivityCalories > 0 -> syncedOnlyText(syncedActivityCalories)
            else -> ""
        }
    }

    private const val MAX_MANUAL_LOGS = 3
}
