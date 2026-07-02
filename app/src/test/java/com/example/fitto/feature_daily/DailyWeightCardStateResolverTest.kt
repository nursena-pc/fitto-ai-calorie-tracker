package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyWeightCardStateResolverTest {

    @Test
    fun `resolve returns unknown for missing weight`() {
        assertEquals(
            DailyWeightCardStateResolver.State.Unknown,
            DailyWeightCardStateResolver.resolve(null)
        )
    }

    @Test
    fun `resolve returns unknown for invalid weight`() {
        assertEquals(
            DailyWeightCardStateResolver.State.Unknown,
            DailyWeightCardStateResolver.resolve(Double.NaN)
        )
        assertEquals(
            DailyWeightCardStateResolver.State.Unknown,
            DailyWeightCardStateResolver.resolve(0.0)
        )
    }

    @Test
    fun `resolve returns available state for valid weight`() {
        assertEquals(
            DailyWeightCardStateResolver.State.Available(72.4),
            DailyWeightCardStateResolver.resolve(72.4)
        )
    }
}
