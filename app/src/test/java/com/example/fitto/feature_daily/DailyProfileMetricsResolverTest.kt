package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyProfileMetricsResolverTest {

    @Test
    fun resolveWeightKg_readsFirstValidWeightCandidate() {
        val profile = ProfileMetricsStub(
            weightKg = 24.0,
            currentWeightKg = 72.5,
            weight = 80.0
        )

        assertEquals(72.5, DailyProfileMetricsResolver.resolveWeightKg(profile)!!, 0.001)
    }

    @Test
    fun resolveWeightKg_parsesCommaDecimalString() {
        val profile = ProfileMetricsStub(
            weightKg = null,
            currentWeightKg = null,
            weight = "68,5"
        )

        assertEquals(68.5, DailyProfileMetricsResolver.resolveWeightKg(profile)!!, 0.001)
    }

    @Test
    fun resolveWeightKg_returnsNullWhenNoCandidateIsValid() {
        val profile = ProfileMetricsStub(
            weightKg = 20.0,
            currentWeightKg = 301.0,
            weight = "unknown"
        )

        assertNull(DailyProfileMetricsResolver.resolveWeightKg(profile))
    }

    @Test
    fun resolveWaterGoalMl_prefersExplicitGoalAndRoundsToBottleSize() {
        val profile = ProfileMetricsStub(
            weightKg = 60.0,
            waterGoalMl = 2101
        )

        assertEquals(2250, DailyProfileMetricsResolver.resolveWaterGoalMl(profile))
    }

    @Test
    fun resolveWaterGoalMl_fallsBackToWeightBasedGoal() {
        val profile = ProfileMetricsStub(
            weightKg = 50.0,
            waterGoalMl = null
        )

        assertEquals(1750, DailyProfileMetricsResolver.resolveWaterGoalMl(profile))
    }

    @Test
    fun resolveWaterGoalMl_withoutValidWeightUsesDefaultWeight() {
        val profile = ProfileMetricsStub(
            weightKg = 20.0,
            waterGoalMl = null
        )

        assertEquals(2500, DailyProfileMetricsResolver.resolveWaterGoalMl(profile))
    }

    private data class ProfileMetricsStub(
        val weightKg: Any? = null,
        val currentWeightKg: Any? = null,
        val weight: Any? = null,
        val currentWeight: Any? = null,
        val initialWeightKg: Any? = null,
        val initialWeight: Any? = null,
        val waterGoalMl: Any? = null,
        val dailyWaterGoalMl: Any? = null,
        val waterTargetMl: Any? = null,
        val waterIntakeGoalMl: Any? = null
    )
}
