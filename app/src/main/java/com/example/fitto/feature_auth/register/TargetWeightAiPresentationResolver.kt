package com.example.fitto.feature_auth.register

import kotlin.math.abs
import kotlin.math.ceil

/**
 * UI-only presentation decisions for the target-weight AI card and explanation sheet.
 * Keeping these calculations outside the Fragment makes the screen easier to test
 * without changing the existing onboarding flow or persistence behavior.
 */
object TargetWeightAiPresentationResolver {

    fun estimatedHealthyDurationWeeks(
        currentWeightKg: Int,
        suggestedTargetWeightKg: Int,
        goalType: GoalType?
    ): IntRange {
        val weightDifferenceKg = abs(currentWeightKg - suggestedTargetWeightKg)
            .coerceAtLeast(MIN_VISIBLE_DIFFERENCE_KG)
            .toDouble()
        val pace = healthyWeeklyPace(goalType)
        val minWeeks = ceil(weightDifferenceKg / pace.fastKgPerWeek)
            .toInt()
            .coerceAtLeast(MIN_VISIBLE_DURATION_WEEKS)
        val maxWeeks = ceil(weightDifferenceKg / pace.slowKgPerWeek)
            .toInt()
            .coerceAtLeast(minWeeks)

        return minWeeks..maxWeeks
    }

    private fun healthyWeeklyPace(goalType: GoalType?): HealthyWeeklyPace {
        return when (goalType) {
            GoalType.LOSE_WEIGHT -> HealthyWeeklyPace(
                slowKgPerWeek = 0.50,
                fastKgPerWeek = 0.75
            )

            GoalType.GAIN_WEIGHT -> HealthyWeeklyPace(
                slowKgPerWeek = 0.25,
                fastKgPerWeek = 0.50
            )

            else -> HealthyWeeklyPace(
                slowKgPerWeek = 0.25,
                fastKgPerWeek = 0.50
            )
        }
    }

    private data class HealthyWeeklyPace(
        val slowKgPerWeek: Double,
        val fastKgPerWeek: Double
    )

    private const val MIN_VISIBLE_DIFFERENCE_KG = 1
    private const val MIN_VISIBLE_DURATION_WEEKS = 2
}
