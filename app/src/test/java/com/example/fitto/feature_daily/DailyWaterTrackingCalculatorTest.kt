package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWaterTrackingCalculatorTest {

    @Test
    fun calculateState_normalizesWaterAndUsesVisibleBottleCount() {
        val state = DailyWaterTrackingCalculator.calculateState(
            rawWaterMl = 749,
            waterGoalMl = 2100,
            visibleBottleCount = 10
        )

        assertEquals(500, state.normalizedWaterMl)
        assertEquals(2, state.filledBottleCount)
        assertEquals(8, state.defaultBottleCount)
        assertEquals(10, state.totalBottleCount)
    }

    @Test
    fun calculateState_expandsTotalBottleCountForFilledBottles() {
        val state = DailyWaterTrackingCalculator.calculateState(
            rawWaterMl = 3000,
            waterGoalMl = 1750,
            visibleBottleCount = 7
        )

        assertEquals(3000, state.normalizedWaterMl)
        assertEquals(12, state.filledBottleCount)
        assertEquals(7, state.defaultBottleCount)
        assertEquals(12, state.totalBottleCount)
    }

    @Test
    fun defaultBottleCount_isBoundedByStoreLimits() {
        assertEquals(7, DailyWaterTrackingCalculator.defaultBottleCount(500))
        assertEquals(10, DailyWaterTrackingCalculator.defaultBottleCount(2500))
        assertEquals(16, DailyWaterTrackingCalculator.defaultBottleCount(6000))
    }

    @Test
    fun normalizeAddAmount_alignsToBottleSize() {
        assertEquals(0, DailyWaterTrackingCalculator.normalizeAddAmount(249))
        assertEquals(250, DailyWaterTrackingCalculator.normalizeAddAmount(250))
        assertEquals(500, DailyWaterTrackingCalculator.normalizeAddAmount(749))
    }

    @Test
    fun toggleBottleWaterMl_addsOrRemovesOneBottle() {
        assertEquals(
            250,
            DailyWaterTrackingCalculator.toggleBottleWaterMl(
                rawWaterMl = 749,
                isFilled = true
            )
        )
        assertEquals(
            750,
            DailyWaterTrackingCalculator.toggleBottleWaterMl(
                rawWaterMl = 749,
                isFilled = false
            )
        )
    }

    @Test
    fun canRemoveVisibleBottle_respectsMinimumAndFilledBottleCount() {
        assertFalse(
            DailyWaterTrackingCalculator.canRemoveVisibleBottle(
                visibleBottleCount = 7,
                rawWaterMl = 500
            )
        )
        assertFalse(
            DailyWaterTrackingCalculator.canRemoveVisibleBottle(
                visibleBottleCount = 8,
                rawWaterMl = 2000
            )
        )
        assertTrue(
            DailyWaterTrackingCalculator.canRemoveVisibleBottle(
                visibleBottleCount = 10,
                rawWaterMl = 500
            )
        )
    }
}
