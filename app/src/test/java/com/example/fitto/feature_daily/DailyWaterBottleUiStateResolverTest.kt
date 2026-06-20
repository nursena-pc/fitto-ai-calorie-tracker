package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWaterBottleUiStateResolverTest {

    @Test
    fun resolve_returnsDisabledRemoveButtonAtMinimumBottleCount() {
        val state = DailyWaterBottleUiStateResolver.resolve(
            DailyWaterTrackingCalculator.State(
                normalizedWaterMl = 0,
                filledBottleCount = 0,
                defaultBottleCount = 7,
                totalBottleCount = 7
            )
        )

        assertFalse(state.removeBottle.isEnabled)
        assertEquals(R.color.fitto_text_secondary, state.removeBottle.tintColorRes)
        assertEquals(0.35f, state.removeBottle.alpha)
        assertEquals(R.string.daily_remove_bottle, state.removeBottle.contentDescriptionRes)
    }

    @Test
    fun resolve_returnsEnabledRemoveButtonAboveMinimumBottleCount() {
        val state = DailyWaterBottleUiStateResolver.resolve(
            DailyWaterTrackingCalculator.State(
                normalizedWaterMl = 0,
                filledBottleCount = 0,
                defaultBottleCount = 8,
                totalBottleCount = 10
            )
        )

        assertTrue(state.removeBottle.isEnabled)
        assertEquals(8, state.removeBottle.defaultBottleCount)
        assertEquals(10, state.removeBottle.visibleBottleCount)
        assertEquals(R.color.fitto_orange, state.removeBottle.tintColorRes)
        assertEquals(1f, state.removeBottle.alpha)
    }

    @Test
    fun resolve_mapsFilledAndEmptyBottleStatesInOrder() {
        val state = DailyWaterBottleUiStateResolver.resolve(
            DailyWaterTrackingCalculator.State(
                normalizedWaterMl = 500,
                filledBottleCount = 2,
                defaultBottleCount = 7,
                totalBottleCount = 4
            )
        )

        assertEquals(listOf(0, 1, 2, 3), state.bottles.map { it.index })
        assertEquals(listOf(1, 2, 3, 4), state.bottles.map { it.number })
        assertEquals(listOf(true, true, false, false), state.bottles.map { it.isFilled })
        assertEquals(
            listOf(
                R.color.fitto_water_blue,
                R.color.fitto_water_blue,
                R.color.fitto_divider_soft,
                R.color.fitto_divider_soft
            ),
            state.bottles.map { it.tintColorRes }
        )
        assertEquals(listOf(1f, 1f, 0.45f, 0.45f), state.bottles.map { it.alpha })
        assertEquals(
            listOf(
                R.string.daily_water_bottle_filled_cd,
                R.string.daily_water_bottle_filled_cd,
                R.string.daily_water_bottle_empty_cd,
                R.string.daily_water_bottle_empty_cd
            ),
            state.bottles.map { it.contentDescriptionRes }
        )
    }
}
