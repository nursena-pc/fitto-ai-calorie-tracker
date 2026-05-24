package com.example.fitto.feature_daily.ai

/**
 * Structured response shown in the Daily Fitto AI card.
 * The app still decides the final card status locally; AI status is informative only.
 */
data class AiDailyAnalysisResult(
    val status: AiDailyStatusLevel,
    val mealComments: List<AiDailyMealComment>,
    val waterComment: String,
    val exerciseComment: String,
    val generalComment: String,
    val suggestion: String,
    val plainText: String? = null
)

data class AiDailyMealComment(
    val mealName: String,
    val comment: String,
    val level: AiDailyStatusLevel
)

enum class AiDailyStatusLevel {
    GREEN,
    YELLOW,
    RED;

    companion object {
        fun from(value: String?): AiDailyStatusLevel {
            return entries.firstOrNull { it.name.equals(value.orEmpty(), ignoreCase = true) }
                ?: GREEN
        }
    }
}
