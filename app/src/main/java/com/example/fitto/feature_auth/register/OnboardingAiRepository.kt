package com.example.fitto.feature_auth.register

interface OnboardingAiRepository {
    suspend fun requestTargetSuggestion(
        prompt: String,
        appLanguage: String
    ): AiTargetSuggestion

    suspend fun requestPlanSuggestion(
        prompt: String,
        appLanguage: String
    ): AiPlanSuggestion
}
