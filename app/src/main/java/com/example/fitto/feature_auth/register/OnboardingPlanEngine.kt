package com.example.fitto.feature_auth.register

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

data class OnboardingPlanValidation(
    val mode: PlanAiMode,
    val displayResult: DailyNeedsResult
)

class OnboardingPlanEngine(
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {

    fun hasBasicUserInfo(state: OnboardingState): Boolean {
        return state.name.isNotBlank() &&
            state.height > 0 &&
            state.weight > 0 &&
            state.age > 0 &&
            state.gender != null
    }

    fun canCalculateDailyNeeds(state: OnboardingState): Boolean {
        val needsTargetWeight = state.goalType == GoalType.LOSE_WEIGHT ||
            state.goalType == GoalType.GAIN_WEIGHT

        return hasBasicUserInfo(state) &&
            state.goalType != null &&
            state.activityLevel != null &&
            (!needsTargetWeight || state.targetWeight > 0)
    }

    fun calculateDailyNeeds(state: OnboardingState): DailyNeedsResult {
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

    fun validateAiTargetSuggestion(
        state: OnboardingState,
        suggestion: AiTargetSuggestion
    ): Boolean {
        val currentWeight = state.weight
        val targetWeight = suggestion.suggestedTargetWeightKg
        val goalType = state.goalType

        if (targetWeight !in MIN_TARGET_WEIGHT_KG..MAX_TARGET_WEIGHT_KG) return false
        if (goalType == GoalType.LOSE_WEIGHT && targetWeight >= currentWeight) return false
        if (goalType == GoalType.GAIN_WEIGHT && targetWeight <= currentWeight) return false
        if (suggestion.riskLevel == AiTargetRiskLevel.NOT_RECOMMENDED) return false

        val safety = TargetWeightSafetyChecker.evaluate(
            age = state.age,
            heightCm = state.height,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            goalType = goalType,
            daysUntilEvent = daysUntilEvent(state),
            aiSuggestedTargetWeightKg = null
        )
        if (safety.status == TargetWeightSafetyStatus.BLOCKED) return false

        val weeklyChangeKg = safety.weeklyChangeKg
        if (weeklyChangeKg != null) {
            if (goalType == GoalType.LOSE_WEIGHT && weeklyChangeKg > MAX_AI_LOSS_KG_PER_WEEK) return false
            if (goalType == GoalType.GAIN_WEIGHT && weeklyChangeKg > MAX_AI_GAIN_KG_PER_WEEK) return false
        }

        return true
    }

    fun validateAiPlanSuggestion(
        state: OnboardingState,
        localResult: DailyNeedsResult,
        suggestion: AiPlanSuggestion
    ): OnboardingPlanValidation {
        val minimumCalories = minimumSafeCalories(state.gender)
        val maxCalories = (localResult.maintenanceCalories * MAX_MAINTENANCE_MULTIPLIER)
            .roundToInt()
            .coerceAtLeast(minimumCalories + MINIMUM_MAX_CALORIE_MARGIN)
        val recommendedCalories = suggestion.recommendedDailyCalories
        val absWeeklyChange = abs(suggestion.recommendedWeeklyChangeKg)
        val isWeightLoss = state.goalType == GoalType.LOSE_WEIGHT
        val isWeightGain = state.goalType == GoalType.GAIN_WEIGHT
        val maxWeeklyChange = when (state.goalType) {
            GoalType.LOSE_WEIGHT -> MAX_VERIFIED_LOSS_KG_PER_WEEK
            GoalType.GAIN_WEIGHT -> MAX_VERIFIED_GAIN_KG_PER_WEEK
            else -> MAX_VERIFIED_MAINTENANCE_KG_PER_WEEK
        }

        val caloriesLooksSafe = recommendedCalories in minimumCalories..maxCalories
        val caloriesCloseToLocal = localResult.appliedCalories <= 0 ||
            abs(recommendedCalories - localResult.appliedCalories) /
            localResult.appliedCalories.toDouble() <= MAX_AI_CALORIE_DIFFERENCE_RATIO
        val weeklyLooksSafe = absWeeklyChange <= maxWeeklyChange
        val planStatusAllowsNumbers = suggestion.planStatus != AiPlanStatus.NOT_RECOMMENDED
        val useAiNumbers = caloriesLooksSafe &&
            caloriesCloseToLocal &&
            weeklyLooksSafe &&
            planStatusAllowsNumbers

        if (!useAiNumbers) {
            return OnboardingPlanValidation(
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
        val planStatus = if (!isWeightChangeGoal) {
            localResult.status
        } else {
            when (suggestion.planStatus) {
                AiPlanStatus.SAFE -> PlanStatus.HEALTHY
                AiPlanStatus.CAUTION, AiPlanStatus.AGGRESSIVE -> PlanStatus.AGGRESSIVE
                AiPlanStatus.NOT_RECOMMENDED -> PlanStatus.NOT_RECOMMENDED
            }
        }

        return OnboardingPlanValidation(
            mode = PlanAiMode.AI_VERIFIED,
            displayResult = localResult.copy(
                targetCalories = recommendedCalories,
                recommendedCalories = recommendedCalories,
                appliedCalories = recommendedCalories,
                isUserTargetApplied = true,
                weeklyWeightChangeKg = signedWeeklyChange,
                estimatedWeeks = estimateWeeksForPlan(state, signedWeeklyChange),
                status = planStatus
            )
        )
    }

    fun daysUntilEvent(state: OnboardingState): Long? {
        val event = state.eventTimeMillis ?: return null
        val days = (event - nowMillis()) / DAY_MILLIS
        return days.takeIf { it > 0L }
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

        val days = (eventTimeMillis - nowMillis()) / DAY_MILLIS.toDouble()
        if (days <= 0.0) return null

        val weeks = days / DAYS_PER_WEEK
        return abs(currentWeight - targetWeight) / weeks
    }

    private fun evaluatePlanStatus(
        goal: GoalType?,
        requiredWeeklyChangeKg: Double?
    ): PlanStatus {
        if (requiredWeeklyChangeKg == null) return PlanStatus.HEALTHY

        return when (goal) {
            GoalType.LOSE_WEIGHT -> when {
                requiredWeeklyChangeKg <= HEALTHY_LOSS_KG_PER_WEEK -> PlanStatus.HEALTHY
                requiredWeeklyChangeKg <= AGGRESSIVE_LOSS_KG_PER_WEEK -> PlanStatus.AGGRESSIVE
                else -> PlanStatus.NOT_RECOMMENDED
            }

            GoalType.GAIN_WEIGHT -> when {
                requiredWeeklyChangeKg <= HEALTHY_GAIN_KG_PER_WEEK -> PlanStatus.HEALTHY
                requiredWeeklyChangeKg <= AGGRESSIVE_GAIN_KG_PER_WEEK -> PlanStatus.AGGRESSIVE
                else -> PlanStatus.NOT_RECOMMENDED
            }

            else -> PlanStatus.NO_WEIGHT_CHANGE
        }
    }

    private fun estimateWeeksForPlan(
        state: OnboardingState,
        weeklyChangeKg: Double
    ): Int? {
        if (state.goalType != GoalType.LOSE_WEIGHT && state.goalType != GoalType.GAIN_WEIGHT) return null
        if (state.targetWeight <= 0 || weeklyChangeKg == 0.0) return null

        val differenceKg = abs(state.weight - state.targetWeight)
        if (differenceKg == 0) return 0
        return ceil(differenceKg / abs(weeklyChangeKg)).toInt()
    }

    private fun weeklyKgToDailyCalories(weeklyKg: Double): Double {
        return weeklyKg * CALORIES_PER_KILOGRAM / DAYS_PER_WEEK
    }

    private fun minimumSafeCalories(gender: Gender?): Int {
        return when (gender) {
            Gender.MALE -> MINIMUM_MALE_CALORIES
            Gender.FEMALE -> MINIMUM_DEFAULT_CALORIES
            Gender.PREFER_NOT_TO_SAY, null -> MINIMUM_DEFAULT_CALORIES
        }
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        const val DAYS_PER_WEEK = 7.0
        const val CALORIES_PER_KILOGRAM = 7700.0
        const val MINIMUM_MALE_CALORIES = 1500
        const val MINIMUM_DEFAULT_CALORIES = 1200
        const val MIN_TARGET_WEIGHT_KG = 25
        const val MAX_TARGET_WEIGHT_KG = 300
        const val HEALTHY_LOSS_KG_PER_WEEK = 0.9
        const val AGGRESSIVE_LOSS_KG_PER_WEEK = 1.2
        const val HEALTHY_GAIN_KG_PER_WEEK = 0.5
        const val AGGRESSIVE_GAIN_KG_PER_WEEK = 0.8
        const val MAX_AI_LOSS_KG_PER_WEEK = 1.5
        const val MAX_AI_GAIN_KG_PER_WEEK = 1.0
        const val MAX_VERIFIED_LOSS_KG_PER_WEEK = 1.2
        const val MAX_VERIFIED_GAIN_KG_PER_WEEK = 0.8
        const val MAX_VERIFIED_MAINTENANCE_KG_PER_WEEK = 0.4
        const val MAX_MAINTENANCE_MULTIPLIER = 1.6
        const val MINIMUM_MAX_CALORIE_MARGIN = 300
        const val MAX_AI_CALORIE_DIFFERENCE_RATIO = 0.25
    }
}
