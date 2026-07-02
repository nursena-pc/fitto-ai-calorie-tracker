package com.example.fitto.feature_daily

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyProfileMetricsLoadActionTest {

    @Test
    fun `load resolves profile weight display weight and water goal together`() = runBlocking {
        var loadCount = 0
        val action = DailyProfileMetricsLoadAction(
            loadProfile = {
                loadCount += 1
                testProfile(
                    userId = "profile",
                    weightKg = 65,
                    waterTargetMl = 2101
                )
            }
        )

        val result = action.load(fallbackWeightKg = 70.0)

        assertEquals(1, loadCount)
        assertEquals(65.0, result.profileWeightKg!!, 0.001)
        assertEquals(65.0, result.displayWeightKg, 0.001)
        assertEquals(2250, result.waterGoalMl)
    }

    @Test
    fun `load uses fallback display weight when profile weight is invalid`() = runBlocking {
        val action = DailyProfileMetricsLoadAction(
            loadProfile = {
                testProfile(
                    userId = "invalid-weight",
                    weightKg = 20
                )
            }
        )

        val result = action.load(fallbackWeightKg = 72.0)

        assertNull(result.profileWeightKg)
        assertEquals(72.0, result.displayWeightKg, 0.001)
        assertEquals(2500, result.waterGoalMl)
    }

    @Test
    fun `load supports missing profile`() = runBlocking {
        val action = DailyProfileMetricsLoadAction(
            loadProfile = { null }
        )

        val result = action.load(fallbackWeightKg = 68.0)

        assertNull(result.profileWeightKg)
        assertEquals(68.0, result.displayWeightKg, 0.001)
        assertEquals(2500, result.waterGoalMl)
    }
}
