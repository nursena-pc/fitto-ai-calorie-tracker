package com.example.fitto.feature_daily

import com.example.fitto.feature_health.HealthConnectDailySummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivityPresentationResolverTest {

    @Test
    fun `display steps prefer permitted Health Connect data`() {
        val steps = DailyActivityPresentationResolver.resolveDisplaySteps(
            healthConnectSummary = healthSummary(
                steps = 4_200,
                hasPermission = true,
                isAvailable = true
            ),
            localSteps = 1_300
        )

        assertEquals(4_200, steps)
    }

    @Test
    fun `display steps fall back to local counter without usable health data`() {
        val steps = DailyActivityPresentationResolver.resolveDisplaySteps(
            healthConnectSummary = healthSummary(
                steps = 4_200,
                hasPermission = false,
                isAvailable = true
            ),
            localSteps = 1_300
        )

        assertEquals(1_300, steps)
    }

    @Test
    fun `source prioritizes Health Connect over local and manual activity`() {
        val source = DailyActivityPresentationResolver.resolveSource(
            healthConnectSummary = healthSummary(
                steps = 100,
                hasPermission = true,
                isAvailable = true
            ),
            healthActiveCalories = 0,
            localSteps = 2_000,
            manualExerciseCalories = 250
        )

        assertEquals(DailyActivityPolicy.Source.HEALTH_CONNECT, source)
    }

    @Test
    fun `source falls through local manual and connect states`() {
        assertEquals(
            DailyActivityPolicy.Source.FITTO_COUNTER,
            DailyActivityPresentationResolver.resolveSource(
                healthConnectSummary = healthSummary(),
                healthActiveCalories = 0,
                localSteps = 2_000,
                manualExerciseCalories = 250
            )
        )
        assertEquals(
            DailyActivityPolicy.Source.MANUAL_ONLY,
            DailyActivityPresentationResolver.resolveSource(
                healthConnectSummary = healthSummary(),
                healthActiveCalories = 0,
                localSteps = 0,
                manualExerciseCalories = 250
            )
        )
        assertEquals(
            DailyActivityPolicy.Source.CONNECT,
            DailyActivityPresentationResolver.resolveSource(
                healthConnectSummary = healthSummary(),
                healthActiveCalories = 0,
                localSteps = 0,
                manualExerciseCalories = 0
            )
        )
    }

    private fun healthSummary(
        steps: Long = 0,
        hasPermission: Boolean = false,
        isAvailable: Boolean = false
    ): HealthConnectDailySummary {
        return HealthConnectDailySummary(
            steps = steps,
            hasPermission = hasPermission,
            isAvailable = isAvailable
        )
    }
}
