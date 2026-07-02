package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileHistoryClearActionTest {

    @Test
    fun `clear runs remote local and notification cleanup in order`() = runBlocking {
        val calls = mutableListOf<String>()
        val action = ProfileHistoryClearAction(
            clearRemoteHistory = {
                calls += "remote"
                "user-1"
            },
            clearLocalHistory = { userId -> calls += "local:$userId" },
            clearNotifications = { calls += "notifications" }
        )

        val result = action.clear()

        assertEquals("user-1", result.userId)
        assertEquals(listOf("remote", "local:user-1", "notifications"), calls)
    }

    @Test
    fun `remote failure prevents local cleanup`() = runBlocking {
        var localCalled = false
        var notificationsCalled = false
        val action = ProfileHistoryClearAction(
            clearRemoteHistory = { error("remote failed") },
            clearLocalHistory = { localCalled = true },
            clearNotifications = { notificationsCalled = true }
        )

        val result = runCatching { action.clear() }

        assertTrue(result.isFailure)
        assertFalse(localCalled)
        assertFalse(notificationsCalled)
    }

    @Test
    fun `local failure prevents notification cleanup`() = runBlocking {
        var notificationsCalled = false
        val action = ProfileHistoryClearAction(
            clearRemoteHistory = { "user-1" },
            clearLocalHistory = { error("local failed") },
            clearNotifications = { notificationsCalled = true }
        )

        val result = runCatching { action.clear() }

        assertTrue(result.isFailure)
        assertFalse(notificationsCalled)
    }
}
