package com.example.fitto.feature_auth.register

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
