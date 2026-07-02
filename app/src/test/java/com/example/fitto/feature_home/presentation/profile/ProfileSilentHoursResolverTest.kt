package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSilentHoursResolverTest {

    @Test
    fun `default value is not marked as custom`() {
        val state = ProfileSilentHoursResolver.choiceState(
            currentValue = "22:00 - 07:00",
            defaultValue = "22:00 - 07:00",
            offValue = "Off"
        )

        assertFalse(state.customRangeSelected)
    }

    @Test
    fun `off value is not marked as custom`() {
        val state = ProfileSilentHoursResolver.choiceState(
            currentValue = "Off",
            defaultValue = "22:00 - 07:00",
            offValue = "Off"
        )

        assertFalse(state.customRangeSelected)
    }

    @Test
    fun `different range is marked as custom`() {
        val state = ProfileSilentHoursResolver.choiceState(
            currentValue = "23:30 - 06:45",
            defaultValue = "22:00 - 07:00",
            offValue = "Off"
        )

        assertTrue(state.customRangeSelected)
    }

    @Test
    fun `valid range is parsed`() {
        val range = ProfileSilentHoursResolver.rangeFor("23:30 - 06:45")

        assertEquals(ProfileSilentHoursRange(23, 30, 6, 45), range)
    }

    @Test
    fun `invalid range falls back to default hours`() {
        val range = ProfileSilentHoursResolver.rangeFor("not-a-range")

        assertEquals(ProfileSilentHoursRange(22, 0, 7, 0), range)
    }

    @Test
    fun `formatted range clamps invalid clock values`() {
        assertEquals(
            "23:00 - 00:59",
            ProfileSilentHoursResolver.formatRange(
                startHour = 25,
                startMinute = -1,
                endHour = -3,
                endMinute = 80
            )
        )
    }
}
