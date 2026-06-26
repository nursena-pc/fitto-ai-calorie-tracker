package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiRequestPolicyTest {

    @Test
    fun resolve_returnsBlockedByCooldownWhenCooldownIsActive() {
        assertEquals(
            DailyAiRequestPolicy.Decision.BlockedByCooldown(remainingSeconds = 2L),
            DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = 2_500L,
                nowMillis = 500L,
                isLoading = false,
                inFlightRequestKey = null,
                requestKey = "today-low-water"
            )
        )
    }

    @Test
    fun resolve_roundsCooldownDownWithMinimumOneSecond() {
        assertEquals(
            DailyAiRequestPolicy.Decision.BlockedByCooldown(remainingSeconds = 1L),
            DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = 1_050L,
                nowMillis = 500L,
                isLoading = false,
                inFlightRequestKey = null,
                requestKey = "today-low-water"
            )
        )
    }

    @Test
    fun resolve_prioritizesCooldownOverLoadingState() {
        assertEquals(
            DailyAiRequestPolicy.Decision.BlockedByCooldown(remainingSeconds = 1L),
            DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = 1_500L,
                nowMillis = 500L,
                isLoading = true,
                inFlightRequestKey = "today-low-water",
                requestKey = "today-low-water"
            )
        )
    }

    @Test
    fun resolve_returnsAlreadyLoadingBeforeSameRequestInFlight() {
        assertEquals(
            DailyAiRequestPolicy.Decision.AlreadyLoading,
            DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = 0L,
                nowMillis = 500L,
                isLoading = true,
                inFlightRequestKey = "today-low-water",
                requestKey = "today-low-water"
            )
        )
    }

    @Test
    fun resolve_returnsSameRequestInFlightWhenRequestKeyMatches() {
        assertEquals(
            DailyAiRequestPolicy.Decision.SameRequestInFlight,
            DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = 0L,
                nowMillis = 500L,
                isLoading = false,
                inFlightRequestKey = "today-low-water",
                requestKey = "today-low-water"
            )
        )
    }

    @Test
    fun resolve_returnsStartWhenNoGuardBlocksRequest() {
        assertEquals(
            DailyAiRequestPolicy.Decision.Start,
            DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = 0L,
                nowMillis = 500L,
                isLoading = false,
                inFlightRequestKey = "yesterday-low-water",
                requestKey = "today-low-water"
            )
        )
    }
}
