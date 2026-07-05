package com.example.fitto.feature_auth.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AiTargetUiState(
    val isAiLoading: Boolean = false,
    val aiTargetSuggestion: AiTargetSuggestion? = null,
    val aiSuggestionContextSignature: String? = null,
    val aiSuggestionLanguage: String? = null,
    val aiError: String? = null
)

class OnboardingViewModel(
    private val aiRepository: OnboardingAiRepository,
    private val planEngine: OnboardingPlanEngine = OnboardingPlanEngine()
) : ViewModel() {

    private val _aiTargetUiState = MutableStateFlow(AiTargetUiState())
    val aiTargetUiState: StateFlow<AiTargetUiState> = _aiTargetUiState.asStateFlow()

    private val _aiPlanUiState = MutableStateFlow(AiPlanUiState())
    val aiPlanUiState: StateFlow<AiPlanUiState> = _aiPlanUiState.asStateFlow()

    var state = OnboardingState()
        private set

    fun saveUserInfo(
        name: String,
        height: Int,
        weight: Int,
        age: Int,
        gender: Gender
    ) {
        val previousState = state
        state = state.copy(
            name = name,
            height = height,
            weight = weight,
            age = age,
            gender = gender
        )
        invalidateAiIfContextChanged(previousState)
    }

    fun saveGoal(goalType: GoalType) {
        val previousState = state
        state = state.copy(goalType = goalType)
        invalidateAiIfContextChanged(previousState)
    }

    fun saveBarriers(
        barriers: List<BarrierType>,
        otherText: String?
    ) {
        val previousState = state
        state = state.copy(
            barriers = barriers,
            otherBarrierText = otherText
        )
        invalidateAiIfContextChanged(previousState)
    }

    fun saveTargetWeight(targetWeight: Int) {
        val previousState = state
        state = state.copy(targetWeight = targetWeight)
        invalidatePlanAiIfContextChanged(previousState)
    }

    fun saveWeightLossReason(
        reason: ReasonType,
        eventTimeMillis: Long?
    ) {
        val previousState = state
        state = state.copy(
            weightLossReason = reason,
            eventTimeMillis = eventTimeMillis
        )
        invalidateAiIfContextChanged(previousState)
    }

    fun saveEventDate(eventTimeMillis: Long?) {
        val previousState = state
        state = state.copy(eventTimeMillis = eventTimeMillis)
        invalidateAiIfContextChanged(previousState)
    }

    fun saveActivityLevel(activityLevel: ActivityLevel) {
        val previousState = state
        state = state.copy(activityLevel = activityLevel)
        invalidateAiIfContextChanged(previousState)
    }

    fun requestAiTargetWeightSuggestion(
        force: Boolean = false,
        appLanguage: String = DEFAULT_AI_LANGUAGE,
        nutritionContext: OnboardingNutritionPromptContext = OnboardingNutritionPromptContext.EMPTY
    ) {
        val promptLanguage = appLanguage.normalizedAiLanguage()
        val contextSignature = state.aiSuggestionContextSignature(nutritionContext)
        val currentUiState = _aiTargetUiState.value
        val hasFreshSuggestion = currentUiState.aiTargetSuggestion != null &&
                currentUiState.aiSuggestionContextSignature == contextSignature &&
                currentUiState.aiSuggestionLanguage == promptLanguage

        if (state.goalType != GoalType.LOSE_WEIGHT && state.goalType != GoalType.GAIN_WEIGHT) {
            _aiTargetUiState.update { it.copy(isAiLoading = false, aiTargetSuggestion = null, aiError = null) }
            return
        }

        if (currentUiState.isAiLoading) return
        if (!force && hasFreshSuggestion) return

        if (!hasBasicUserInfo() || state.goalType == null || state.activityLevel == null) {
            Log.w(
                FITTO_AI_LOG_TAG,
                "Target suggestion skipped: missing onboarding info. " +
                    "hasBasic=${hasBasicUserInfo()}, goal=${state.goalType}, activity=${state.activityLevel}"
            )
            _aiTargetUiState.update {
                it.copy(
                    isAiLoading = false,
                    aiError = "missing_onboarding_info"
                )
            }
            return
        }

        viewModelScope.launch {
            _aiTargetUiState.update {
                it.copy(
                    isAiLoading = true,
                    aiTargetSuggestion = if (hasFreshSuggestion && !force) it.aiTargetSuggestion else null,
                    aiSuggestionContextSignature = if (hasFreshSuggestion && !force) it.aiSuggestionContextSignature else null,
                    aiSuggestionLanguage = if (hasFreshSuggestion && !force) it.aiSuggestionLanguage else null,
                    aiError = null
                )
            }

            runCatching {
                Log.d(FITTO_AI_LOG_TAG, "Target suggestion request started.")
                val prompt = AiTargetPromptBuilder.build(
                    state = state,
                    appLanguage = promptLanguage,
                    nutritionContext = nutritionContext
                )
                aiRepository.requestTargetSuggestion(
                    prompt = prompt,
                    appLanguage = promptLanguage
                )
            }.onSuccess { suggestion ->
                Log.d(FITTO_AI_LOG_TAG, "Target suggestion received: ${suggestion.suggestedTargetWeightKg} kg")
                val validatedSuggestion = suggestion.withPersonalizedExplanation(promptLanguage)
                    .takeIf { planEngine.validateAiTargetSuggestion(state, it) }
                _aiTargetUiState.update { current ->
                    if (state.aiSuggestionContextSignature(nutritionContext) == contextSignature) {
                        if (validatedSuggestion != null) {
                            current.copy(
                                isAiLoading = false,
                                aiTargetSuggestion = validatedSuggestion,
                                aiSuggestionContextSignature = contextSignature,
                                aiSuggestionLanguage = promptLanguage,
                                aiError = null
                            )
                        } else {
                            current.copy(
                                isAiLoading = false,
                                aiTargetSuggestion = null,
                                aiSuggestionContextSignature = contextSignature,
                                aiSuggestionLanguage = promptLanguage,
                                aiError = "unsafe_ai_suggestion"
                            )
                        }
                    } else {
                        current
                    }
                }
            }.onFailure { throwable ->
                val errorCode = throwable.toAiErrorCode()
                Log.w(FITTO_AI_LOG_TAG, "Target suggestion failed: $errorCode - ${throwable.message}")
                _aiTargetUiState.update { current ->
                    if (state.aiSuggestionContextSignature(nutritionContext) == contextSignature) {
                        current.copy(
                            isAiLoading = false,
                            aiError = errorCode
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun requestAiPlanSuggestion(
        localResult: DailyNeedsResult,
        force: Boolean = false,
        appLanguage: String = DEFAULT_AI_LANGUAGE,
        nutritionContext: OnboardingNutritionPromptContext = OnboardingNutritionPromptContext.EMPTY
    ) {
        val promptLanguage = appLanguage.normalizedAiLanguage()
        val contextSignature = state.aiPlanContextSignature(localResult, nutritionContext)
        val currentUiState = _aiPlanUiState.value
        val hasFreshPlan = currentUiState.planContextSignature == contextSignature &&
                currentUiState.planLanguage == promptLanguage &&
                (currentUiState.aiPlanSuggestion != null || currentUiState.aiError != null)

        if (currentUiState.isLoading) return
        if (!force && hasFreshPlan) return
        if (!canCalculateDailyNeeds()) return

        _aiPlanUiState.update {
            it.copy(
                isLoading = true,
                planMode = PlanAiMode.LOADING,
                aiError = null,
                planContextSignature = contextSignature,
                planLanguage = promptLanguage
            )
        }

        viewModelScope.launch {
            runCatching {
                Log.d(FITTO_AI_LOG_TAG, "Plan suggestion request started.")
                val prompt = AiPlanPromptBuilder.build(
                    state = state,
                    localResult = localResult,
                    appLanguage = promptLanguage,
                    nutritionContext = nutritionContext
                )
                aiRepository.requestPlanSuggestion(prompt, promptLanguage)
            }.onSuccess { suggestion ->
                val validation = planEngine.validateAiPlanSuggestion(state, localResult, suggestion)
                _aiPlanUiState.update { current ->
                    if (state.aiPlanContextSignature(localResult, nutritionContext) == contextSignature) {
                        current.copy(
                            isLoading = false,
                            planMode = validation.mode,
                            aiPlanSuggestion = suggestion,
                            displayResult = validation.displayResult,
                            planContextSignature = contextSignature,
                            planLanguage = promptLanguage,
                            aiError = null
                        )
                    } else {
                        current
                    }
                }
            }.onFailure { throwable ->
                val errorCode = throwable.toAiErrorCode()
                Log.w(FITTO_AI_LOG_TAG, "Plan suggestion failed: $errorCode - ${throwable.message}")
                _aiPlanUiState.update { current ->
                    if (state.aiPlanContextSignature(localResult, nutritionContext) == contextSignature) {
                        current.copy(
                            isLoading = false,
                            planMode = PlanAiMode.STANDARD,
                            aiPlanSuggestion = null,
                            displayResult = localResult,
                            planContextSignature = contextSignature,
                            planLanguage = promptLanguage,
                            aiError = errorCode
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun clearAiError() {
        _aiTargetUiState.update { it.copy(aiError = null) }
    }

    fun currentAiSuggestionContextSignature(
        nutritionContext: OnboardingNutritionPromptContext = OnboardingNutritionPromptContext.EMPTY
    ): String {
        return state.aiSuggestionContextSignature(nutritionContext)
    }

    fun isAiTargetSuggestionFresh(
        appLanguage: String = DEFAULT_AI_LANGUAGE,
        nutritionContext: OnboardingNutritionPromptContext = OnboardingNutritionPromptContext.EMPTY
    ): Boolean {
        val promptLanguage = appLanguage.normalizedAiLanguage()
        val uiState = _aiTargetUiState.value
        return uiState.aiTargetSuggestion != null &&
                uiState.aiSuggestionContextSignature == state.aiSuggestionContextSignature(nutritionContext) &&
                uiState.aiSuggestionLanguage == promptLanguage
    }

    private fun invalidateAiIfContextChanged(previousState: OnboardingState) {
        if (previousState.basicTargetContextSignature() == state.basicTargetContextSignature()) return

        _aiTargetUiState.update {
            it.copy(
                isAiLoading = false,
                aiTargetSuggestion = null,
                aiSuggestionContextSignature = null,
                aiSuggestionLanguage = null,
                aiError = null
            )
        }
        invalidatePlanAiIfContextChanged(previousState)
    }

    private fun invalidatePlanAiIfContextChanged(previousState: OnboardingState) {
        if (previousState.basicPlanContextSignature() == state.basicPlanContextSignature()) return
        _aiPlanUiState.update { AiPlanUiState() }
    }

    private fun OnboardingState.basicTargetContextSignature(): String {
        return listOf(
            name.trim(), height, weight, age, gender?.name.orEmpty(), goalType?.name.orEmpty(),
            barriers.joinToString(separator = ",") { it.name }, otherBarrierText.orEmpty().trim(),
            weightLossReason?.name.orEmpty(), eventTimeMillis?.toString().orEmpty(), activityLevel?.name.orEmpty(),
            targetWeight
        ).joinToString(separator = "|")
    }

    private fun OnboardingState.basicPlanContextSignature(): String {
        return basicTargetContextSignature()
    }

    private fun OnboardingState.aiSuggestionContextSignature(
        nutritionContext: OnboardingNutritionPromptContext
    ): String {
        return basicTargetContextSignature() + "|nutrition=${nutritionContext.signature()}"
    }

    private fun OnboardingState.aiPlanContextSignature(
        localResult: DailyNeedsResult,
        nutritionContext: OnboardingNutritionPromptContext
    ): String {
        return listOf(
            basicPlanContextSignature(),
            nutritionContext.signature(),
            localResult.maintenanceCalories,
            localResult.targetCalories,
            localResult.appliedCalories,
            localResult.weeklyWeightChangeKg,
            localResult.status.name
        ).joinToString(separator = "|")
    }

    private fun AiTargetSuggestion.withPersonalizedExplanation(appLanguage: String): AiTargetSuggestion {
        return copy(explanation = ensureUserNamePrefix(explanation, appLanguage))
    }

    private fun ensureUserNamePrefix(text: String, appLanguage: String): String {
        val displayName = state.aiDisplayName(appLanguage)
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return "$displayName."

        return if (normalizedText.containsName(displayName)) {
            normalizedText
        } else {
            "$displayName, $normalizedText"
        }
    }

    private fun OnboardingState.aiDisplayName(appLanguage: String): String {
        return name
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: if (appLanguage == AI_LANGUAGE_EN) "Fitto user" else "Fitto kullanıcısı"
    }

    private fun String.containsName(displayName: String): Boolean {
        return contains(displayName, ignoreCase = true)
    }

    private fun Throwable.toAiErrorCode(): String {
        val messageText = message.orEmpty()
        return when {
            messageText.contains("missing_api_key", ignoreCase = true) -> "missing_api_key"
            messageText.contains("Unable to resolve host", ignoreCase = true) -> "network_error"
            messageText.contains("timeout", ignoreCase = true) -> "network_error"
            messageText.contains("valid JSON", ignoreCase = true) -> "invalid_response"
            messageText.contains("invalid target weight", ignoreCase = true) -> "invalid_response"
            messageText.contains("gemini_http_429", ignoreCase = true) -> "rate_limited"
            else -> "ai_target_suggestion_failed"
        }
    }

    fun hasBasicUserInfo(): Boolean {
        return planEngine.hasBasicUserInfo(state)
    }

    fun canCalculateDailyNeeds(): Boolean {
        return planEngine.canCalculateDailyNeeds(state)
    }

    fun calculateDailyNeeds(): DailyNeedsResult {
        return planEngine.calculateDailyNeeds(state)
    }

    private fun String.normalizedAiLanguage(): String {
        return if (startsWith(AI_LANGUAGE_EN, ignoreCase = true)) {
            AI_LANGUAGE_EN
        } else {
            AI_LANGUAGE_TR
        }
    }

    private companion object {
        const val FITTO_AI_LOG_TAG = "FittoAI"
        const val DEFAULT_AI_LANGUAGE = "tr"
        const val AI_LANGUAGE_TR = "tr"
        const val AI_LANGUAGE_EN = "en"
    }
}
