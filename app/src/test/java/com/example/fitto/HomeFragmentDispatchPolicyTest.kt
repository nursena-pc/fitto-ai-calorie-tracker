package com.example.fitto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFragmentDispatchPolicyTest {

    @Test
    fun `delay is immediate for first lookup and delayed for retries`() {
        assertEquals(0L, HomeFragmentDispatchPolicy.delayMillis(attempt = 0))
        assertEquals(120L, HomeFragmentDispatchPolicy.delayMillis(attempt = 1))
        assertEquals(120L, HomeFragmentDispatchPolicy.delayMillis(attempt = 6))
    }

    @Test
    fun `retry policy preserves original six retry boundary`() {
        assertTrue(HomeFragmentDispatchPolicy.shouldRetry(attempt = 0))
        assertTrue(HomeFragmentDispatchPolicy.shouldRetry(attempt = 5))
        assertFalse(HomeFragmentDispatchPolicy.shouldRetry(attempt = 6))
    }
}
