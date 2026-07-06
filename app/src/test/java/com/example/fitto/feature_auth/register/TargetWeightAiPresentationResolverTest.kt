package com.example.fitto.feature_auth.register

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetWeightAiPresentationResolverTest {

    @Test
    fun estimatedHealthyDurationWeeks_forWeightLossUsesBalancedPaceRange() {
        val duration = TargetWeightAiPresentationResolver.estimatedHealthyDurationWeeks(
            currentWeightKg = 74,
            suggestedTargetWeightKg = 68,
            goalType = GoalType.LOSE_WEIGHT
        )

        assertEquals(8..12, duration)
    }

    @Test
    fun estimatedHealthyDurationWeeks_forWeightGainUsesSlowerPaceRange() {
        val duration = TargetWeightAiPresentationResolver.estimatedHealthyDurationWeeks(
            currentWeightKg = 60,
            suggestedTargetWeightKg = 64,
            goalType = GoalType.GAIN_WEIGHT
        )

        assertEquals(8..16, duration)
    }
}
