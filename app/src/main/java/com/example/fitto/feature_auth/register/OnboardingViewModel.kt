package com.example.fitto.feature_auth.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt


data class OnboardingState(
    val name: String = "",
    val height: Int = 0,
    val weight: Int = 0,
    val age: Int = 0,
    val gender: Gender? = null,
    val goalType: GoalType? = null,
    val barriers: List<BarrierType> = emptyList(),
    val otherBarrierText: String? = null,
    val weightLossReason: ReasonType? = null,
    val targetWeight: Int = 0,
    val eventTimeMillis: Long? = null,
    val activityLevel: ActivityLevel? = null
)

enum class PlanStatus {
    HEALTHY,
    AGGRESSIVE,
    NOT_RECOMMENDED,
    NO_WEIGHT_CHANGE
}

data class AiTargetUiState(
    val isAiLoading: Boolean = false,
    val aiTargetSuggestion: AiTargetSuggestion? = null,
    val aiSuggestionContextSignature: String? = null,
    val aiSuggestionLanguage: String? = null,
    val aiError: String? = null
)

data class DailyNeedsResult(
    val bmr: Int,
    val maintenanceCalories: Int,
    val targetCalories: Int,
    val recommendedCalories: Int,
    val appliedCalories: Int,
    val isUserTargetApplied: Boolean,
    val weeklyWeightChangeKg: Double,
    val estimatedWeeks: Int?,
    val requiredWeeklyChangeKg: Double?,
    val status: PlanStatus
)

class OnboardingViewModel : ViewModel() {

    private val aiRepository = GeminiAiRepository()

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
                    .takeIf { validateAiTargetSuggestion(it) }
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
                val validation = validateAiPlanSuggestion(localResult, suggestion)
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

    private fun validateAiTargetSuggestion(suggestion: AiTargetSuggestion): Boolean {
        val currentWeight = state.weight
        val targetWeight = suggestion.suggestedTargetWeightKg
        val goalType = state.goalType

        if (targetWeight !in 25..300) return false
        if (goalType == GoalType.LOSE_WEIGHT && targetWeight >= currentWeight) return false
        if (goalType == GoalType.GAIN_WEIGHT && targetWeight <= currentWeight) return false
        if (suggestion.riskLevel == AiTargetRiskLevel.NOT_RECOMMENDED) return false

        val safety = TargetWeightSafetyChecker.evaluate(
            age = state.age,
            heightCm = state.height,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            goalType = goalType,
            daysUntilEvent = state.daysUntilEvent(),
            aiSuggestedTargetWeightKg = null
        )
        if (safety.status == TargetWeightSafetyStatus.BLOCKED) return false

        val weeklyChangeKg = safety.weeklyChangeKg
        if (weeklyChangeKg != null) {
            if (goalType == GoalType.LOSE_WEIGHT && weeklyChangeKg > 1.5) return false
            if (goalType == GoalType.GAIN_WEIGHT && weeklyChangeKg > 1.0) return false
        }

        return true
    }

    private data class PlanValidation(
        val mode: PlanAiMode,
        val displayResult: DailyNeedsResult
    )

    private fun validateAiPlanSuggestion(
        localResult: DailyNeedsResult,
        suggestion: AiPlanSuggestion
    ): PlanValidation {
        val minimumCalories = minimumSafeCalories(state.gender)
        val maxCalories = (localResult.maintenanceCalories * 1.6).roundToInt().coerceAtLeast(minimumCalories + 300)
        val recommendedCalories = suggestion.recommendedDailyCalories
        val absWeeklyChange = abs(suggestion.recommendedWeeklyChangeKg)
        val isWeightLoss = state.goalType == GoalType.LOSE_WEIGHT
        val isWeightGain = state.goalType == GoalType.GAIN_WEIGHT
        val maxWeeklyChange = when (state.goalType) {
            GoalType.LOSE_WEIGHT -> 1.2
            GoalType.GAIN_WEIGHT -> 0.8
            else -> 0.4
        }

        val caloriesLooksSafe = recommendedCalories in minimumCalories..maxCalories
        val caloriesCloseToLocal = localResult.appliedCalories <= 0 ||
                abs(recommendedCalories - localResult.appliedCalories) / localResult.appliedCalories.toDouble() <= 0.25
        val weeklyLooksSafe = absWeeklyChange <= maxWeeklyChange
        val planStatusAllowsNumbers = suggestion.planStatus != AiPlanStatus.NOT_RECOMMENDED

        val useAiNumbers = caloriesLooksSafe && caloriesCloseToLocal && weeklyLooksSafe && planStatusAllowsNumbers

        if (!useAiNumbers) {
            return PlanValidation(
                mode = PlanAiMode.AI_ADJUSTED,
                displayResult = localResult
            )
        }

        val isWeightChangeGoal = isWeightLoss || isWeightGain
        val signedWeeklyChange = when {
            isWeightLoss -> -absWeeklyChange
            isWeightGain -> absWeeklyChange
            else -> 0.0
        }

        val estimatedWeeks = estimateWeeksForPlan(signedWeeklyChange)
        val planStatus = if (!isWeightChangeGoal) {
            // Maintain / learn nutrition / eat healthier hedeflerinde kilo değişimi statüsü göstermeyelim.
            // AI yorumu planı zenginleştirir; kullanıcıya görünen yerel hedef statüsü korunur.
            localResult.status
        } else {
            when (suggestion.planStatus) {
                AiPlanStatus.SAFE -> PlanStatus.HEALTHY
                AiPlanStatus.CAUTION, AiPlanStatus.AGGRESSIVE -> PlanStatus.AGGRESSIVE
                AiPlanStatus.NOT_RECOMMENDED -> PlanStatus.NOT_RECOMMENDED
            }
        }

        return PlanValidation(
            mode = PlanAiMode.AI_VERIFIED,
            displayResult = localResult.copy(
                targetCalories = recommendedCalories,
                recommendedCalories = recommendedCalories,
                appliedCalories = recommendedCalories,
                isUserTargetApplied = true,
                weeklyWeightChangeKg = signedWeeklyChange,
                estimatedWeeks = estimatedWeeks,
                status = planStatus
            )
        )
    }

    private fun estimateWeeksForPlan(weeklyChangeKg: Double): Int? {
        if (state.goalType != GoalType.LOSE_WEIGHT && state.goalType != GoalType.GAIN_WEIGHT) return null
        if (state.targetWeight <= 0 || weeklyChangeKg == 0.0) return null
        val diff = abs(state.weight - state.targetWeight)
        if (diff == 0) return 0
        return ceil(diff / abs(weeklyChangeKg)).toInt()
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
        return state.name.isNotBlank() &&
                state.height > 0 &&
                state.weight > 0 &&
                state.age > 0 &&
                state.gender != null
    }

    fun canCalculateDailyNeeds(): Boolean {
        val needsTargetWeight = state.goalType == GoalType.LOSE_WEIGHT ||
                state.goalType == GoalType.GAIN_WEIGHT

        return hasBasicUserInfo() &&
                state.goalType != null &&
                state.activityLevel != null &&
                (!needsTargetWeight || state.targetWeight > 0)
    }

    fun calculateDailyNeeds(): DailyNeedsResult {
        val bmr = calculateBmr(
            weight = state.weight,
            height = state.height,
            age = state.age,
            gender = state.gender
        )

        val maintenanceCalories =
            (bmr * activityMultiplier(state.activityLevel)).roundToInt()

        val isWeightLoss = state.goalType == GoalType.LOSE_WEIGHT
        val isWeightGain = state.goalType == GoalType.GAIN_WEIGHT

        if (!isWeightLoss && !isWeightGain) {
            return DailyNeedsResult(
                bmr = bmr.roundToInt(),
                maintenanceCalories = maintenanceCalories,
                targetCalories = maintenanceCalories,
                recommendedCalories = maintenanceCalories,
                appliedCalories = maintenanceCalories,
                isUserTargetApplied = true,
                weeklyWeightChangeKg = 0.0,
                estimatedWeeks = null,
                requiredWeeklyChangeKg = null,
                status = PlanStatus.NO_WEIGHT_CHANGE
            )
        }

        val weightDifference = abs(state.weight - state.targetWeight)

        val requiredWeeklyChangeKg = calculateRequiredWeeklyChange(
            currentWeight = state.weight,
            targetWeight = state.targetWeight,
            eventTimeMillis = state.eventTimeMillis
        )

        val recommendedWeeklyChangeKg = if (isWeightLoss) 0.5 else 0.3
        val targetWeeklyChangeKg = requiredWeeklyChangeKg ?: recommendedWeeklyChangeKg

        val targetDailyCalorieChange = weeklyKgToDailyCalories(targetWeeklyChangeKg)
        val minimumCalories = minimumSafeCalories(state.gender)

        val rawTargetCalories = if (isWeightLoss) {
            maintenanceCalories - targetDailyCalorieChange
        } else {
            maintenanceCalories + targetDailyCalorieChange
        }

        val targetCalories = rawTargetCalories
            .roundToInt()
            .coerceAtLeast(minimumCalories)

        val recommendedCalories = if (isWeightLoss) {
            (maintenanceCalories - 500).coerceAtLeast(minimumCalories)
        } else {
            maintenanceCalories + 300
        }

        val status = evaluatePlanStatus(
            goal = state.goalType,
            requiredWeeklyChangeKg = requiredWeeklyChangeKg
        )

        val isUserTargetApplied =
            status == PlanStatus.HEALTHY || status == PlanStatus.NO_WEIGHT_CHANGE

        val appliedCalories = if (isUserTargetApplied) {
            targetCalories
        } else {
            recommendedCalories
        }

        val appliedWeeklyChangeKg = if (isUserTargetApplied) {
            if (isWeightLoss) -targetWeeklyChangeKg else targetWeeklyChangeKg
        } else {
            if (isWeightLoss) -recommendedWeeklyChangeKg else recommendedWeeklyChangeKg
        }

        val estimatedWeeks = if (weightDifference == 0) {
            0
        } else {
            ceil(weightDifference / abs(appliedWeeklyChangeKg)).toInt()
        }

        return DailyNeedsResult(
            bmr = bmr.roundToInt(),
            maintenanceCalories = maintenanceCalories,
            targetCalories = targetCalories,
            recommendedCalories = recommendedCalories,
            appliedCalories = appliedCalories,
            isUserTargetApplied = isUserTargetApplied,
            weeklyWeightChangeKg = appliedWeeklyChangeKg,
            estimatedWeeks = estimatedWeeks,
            requiredWeeklyChangeKg = requiredWeeklyChangeKg,
            status = status
        )
    }

    private fun calculateBmr(
        weight: Int,
        height: Int,
        age: Int,
        gender: Gender?
    ): Double {
        return when (gender) {
            Gender.MALE -> 10 * weight + 6.25 * height - 5 * age + 5
            Gender.FEMALE -> 10 * weight + 6.25 * height - 5 * age - 161
            Gender.PREFER_NOT_TO_SAY, null -> 10 * weight + 6.25 * height - 5 * age - 78
        }
    }

    private fun activityMultiplier(activityLevel: ActivityLevel?): Double {
        return when (activityLevel) {
            ActivityLevel.LOW -> 1.2
            ActivityLevel.MODERATE -> 1.375
            ActivityLevel.HIGH -> 1.55
            ActivityLevel.VERY_HIGH -> 1.725
            null -> 1.2
        }
    }

    private fun calculateRequiredWeeklyChange(
        currentWeight: Int,
        targetWeight: Int,
        eventTimeMillis: Long?
    ): Double? {
        if (eventTimeMillis == null) return null

        val diffMillis = eventTimeMillis - System.currentTimeMillis()
        val days = diffMillis / (1000.0 * 60.0 * 60.0 * 24.0)

        if (days <= 0) return null

        val weeks = days / 7.0
        val weightDifference = abs(currentWeight - targetWeight)

        return weightDifference / weeks
    }

    private fun evaluatePlanStatus(
        goal: GoalType?,
        requiredWeeklyChangeKg: Double?
    ): PlanStatus {
        if (requiredWeeklyChangeKg == null) return PlanStatus.HEALTHY

        return when (goal) {
            GoalType.LOSE_WEIGHT -> when {
                requiredWeeklyChangeKg <= 0.9 -> PlanStatus.HEALTHY
                requiredWeeklyChangeKg <= 1.2 -> PlanStatus.AGGRESSIVE
                else -> PlanStatus.NOT_RECOMMENDED
            }

            GoalType.GAIN_WEIGHT -> when {
                requiredWeeklyChangeKg <= 0.5 -> PlanStatus.HEALTHY
                requiredWeeklyChangeKg <= 0.8 -> PlanStatus.AGGRESSIVE
                else -> PlanStatus.NOT_RECOMMENDED
            }

            else -> PlanStatus.NO_WEIGHT_CHANGE
        }
    }

    private fun weeklyKgToDailyCalories(weeklyKg: Double): Double {
        return weeklyKg * 7700.0 / 7.0
    }

    private fun minimumSafeCalories(gender: Gender?): Int {
        return when (gender) {
            Gender.MALE -> 1500
            Gender.FEMALE -> 1200
            Gender.PREFER_NOT_TO_SAY, null -> 1200
        }
    }

    private fun OnboardingState.daysUntilEvent(): Long? {
        val event = eventTimeMillis ?: return null
        val days = ((event - System.currentTimeMillis()) / (1000L * 60L * 60L * 24L))
        return days.takeIf { it > 0L }
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
