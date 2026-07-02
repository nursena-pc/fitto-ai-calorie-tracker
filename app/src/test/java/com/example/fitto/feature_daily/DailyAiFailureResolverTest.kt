package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiFailureResolverTest {

    @Test
    fun `network error is resolved explicitly`() {
        assertEquals(
            DailyAiFailureKind.NETWORK,
            DailyAiFailureResolver.resolve(RuntimeException("network_error"))
        )
    }

    @Test
    fun `rate limit accepts repository and generic 429 messages`() {
        assertEquals(
            DailyAiFailureKind.RATE_LIMIT,
            DailyAiFailureResolver.resolve(RuntimeException("gemini_http_429"))
        )
        assertEquals(
            DailyAiFailureKind.RATE_LIMIT,
            DailyAiFailureResolver.resolve(RuntimeException("HTTP 429"))
        )
    }

    @Test
    fun `unknown error is general`() {
        assertEquals(
            DailyAiFailureKind.GENERAL,
            DailyAiFailureResolver.resolve(RuntimeException("unknown"))
        )
    }
}
