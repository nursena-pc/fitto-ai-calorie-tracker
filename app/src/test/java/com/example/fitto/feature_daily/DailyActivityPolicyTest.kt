package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivityPolicyTest {

    @Test
    fun resolveDisplaySteps_prefersHealthStepsWhenPermissionExists() {
        val steps = DailyActivityPolicy.resolveDisplaySteps(
            isHealthConnectAvailable = true,
            hasHealthConnectPermission = true,
            healthSteps = 3200,
            localSteps = 1200
        )

        assertEquals(3200, steps)
    }

    @Test
    fun resolveDisplaySteps_fallsBackToLocalSteps() {
        val steps = DailyActivityPolicy.resolveDisplaySteps(
            isHealthConnectAvailable = true,
            hasHealthConnectPermission = false,
            healthSteps = 3200,
            localSteps = 1200
        )

        assertEquals(1200, steps)
    }

    @Test
    fun resolveDisplaySteps_clampsNegativeValues() {
        val steps = DailyActivityPolicy.resolveDisplaySteps(
            isHealthConnectAvailable = false,
            hasHealthConnectPermission = false,
            healthSteps = -10,
            localSteps = -20
        )

        assertEquals(0, steps)
    }

    @Test
    fun resolveBurnedCalories_usesLargestSafeValue() {
        assertEquals(
            350,
            DailyActivityPolicy.resolveBurnedCalories(
                healthActiveCalories = 350,
                manualExerciseCalories = 120
            )
        )
        assertEquals(
            120,
            DailyActivityPolicy.resolveBurnedCalories(
                healthActiveCalories = -1,
                manualExerciseCalories = 120
            )
        )
    }

    @Test
    fun resolveActivitySource_prioritizesHealthThenLocalThenManual() {
        assertEquals(
            DailyActivityPolicy.Source.HEALTH_CONNECT,
            DailyActivityPolicy.resolveActivitySource(
                isHealthConnectAvailable = true,
                hasHealthConnectPermission = true,
                healthSteps = 100,
                healthActiveCalories = 0,
                localSteps = 1000,
                manualExerciseCalories = 300
            )
        )
        assertEquals(
            DailyActivityPolicy.Source.FITTO_COUNTER,
            DailyActivityPolicy.resolveActivitySource(
                isHealthConnectAvailable = true,
                hasHealthConnectPermission = false,
                healthSteps = 100,
                healthActiveCalories = 0,
                localSteps = 1000,
                manualExerciseCalories = 300
            )
        )
        assertEquals(
            DailyActivityPolicy.Source.MANUAL_ONLY,
            DailyActivityPolicy.resolveActivitySource(
                isHealthConnectAvailable = false,
                hasHealthConnectPermission = false,
                healthSteps = 0,
                healthActiveCalories = 0,
                localSteps = 0,
                manualExerciseCalories = 300
            )
        )
        assertEquals(
            DailyActivityPolicy.Source.CONNECT,
            DailyActivityPolicy.resolveActivitySource(
                isHealthConnectAvailable = false,
                hasHealthConnectPermission = false,
                healthSteps = 0,
                healthActiveCalories = 0,
                localSteps = 0,
                manualExerciseCalories = 0
            )
        )
    }
}
