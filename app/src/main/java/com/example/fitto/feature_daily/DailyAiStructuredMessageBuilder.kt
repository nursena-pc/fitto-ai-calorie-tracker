package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult

object DailyAiStructuredMessageBuilder {

    data class Labels(
        val mealComments: String,
        val unknownEntry: String,
        val water: String,
        val exercise: String,
        val generalComment: String,
        val suggestion: String
    )

    fun build(
        analysis: AiDailyAnalysisResult,
        labels: Labels
    ): String {
        analysis.plainText
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let { return it }

        return buildString {
            val usefulMealComments = analysis.mealComments.filter { it.comment.isNotBlank() }
            if (usefulMealComments.isNotEmpty()) {
                append(labels.mealComments).append('\n')
                usefulMealComments.forEach { meal ->
                    append("\u2022 ")
                        .append(meal.mealName.trim().ifBlank { labels.unknownEntry })
                        .append(": ")
                        .append(meal.comment.trim())
                        .append('\n')
                }
            }

            appendSection(labels.water, analysis.waterComment)
            appendSection(labels.exercise, analysis.exerciseComment)
            appendSection(labels.generalComment, analysis.generalComment)
            appendSection(labels.suggestion, analysis.suggestion)
        }.trim()
    }

    private fun StringBuilder.appendSection(label: String, value: String) {
        val normalizedValue = value.trim()
        if (normalizedValue.isBlank()) return

        if (isNotEmpty()) append('\n')
        append(label).append('\n')
        append(normalizedValue).append('\n')
    }
}
