package com.example.fitto.feature_auth.register

enum class AiPlanStatus {
    SAFE,
    CAUTION,
    AGGRESSIVE,
    NOT_RECOMMENDED;

    companion object {
        fun fromRaw(value: String?): AiPlanStatus {
            return entries.firstOrNull { it.name.equals(value.orEmpty(), ignoreCase = true) } ?: SAFE
        }
    }
}

enum class PlanAiMode {
    LOADING,
    STANDARD,
    AI_VERIFIED,
    AI_ADJUSTED
}

data class AiPlanSuggestion(
    val planStatus: AiPlanStatus,
    val recommendedDailyCalories: Int,
    val recommendedWeeklyChangeKg: Double,
    val targetComment: String,
    val timelineComment: String,
    val calorieComment: String,
    val motivationComment: String,
    val summary: String,
    val warning: String
)

data class AiPlanUiState(
    val isLoading: Boolean = false,
    val planMode: PlanAiMode = PlanAiMode.STANDARD,
    val aiPlanSuggestion: AiPlanSuggestion? = null,
    val displayResult: DailyNeedsResult? = null,
    val planContextSignature: String? = null,
    val planLanguage: String? = null,
    val aiError: String? = null
)
