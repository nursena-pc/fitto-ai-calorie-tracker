package com.example.fitto.feature_auth.register

enum class AiTargetRiskLevel {
    SAFE,
    CAUTION,
    NOT_RECOMMENDED;

    companion object {
        fun fromRaw(value: String?): AiTargetRiskLevel {
            return entries.firstOrNull { it.name.equals(value.orEmpty(), ignoreCase = true) } ?: SAFE
        }
    }
}

data class AiTargetSuggestion(
    val suggestedTargetWeightKg: Int,
    val suggestedTargetRangeMinKg: Int? = null,
    val suggestedTargetRangeMaxKg: Int? = null,
    val title: String,
    val explanation: String,
    val timelineComment: String = "",
    val motivationComment: String = "",
    val riskLevel: AiTargetRiskLevel = AiTargetRiskLevel.SAFE,
    val reasons: List<String> = emptyList(),
    val alternativeTargetsKg: List<Int> = emptyList(),
    val warning: String
)
