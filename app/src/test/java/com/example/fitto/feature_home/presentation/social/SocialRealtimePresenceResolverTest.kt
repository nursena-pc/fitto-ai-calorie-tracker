package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialRealtimePresenceResolverTest {

    @Test
    fun shouldSkipUserPresence_returnsTrueForBlankUserId() {
        assertTrue(SocialRealtimePresenceResolver.shouldSkipUserPresence(" "))
        assertFalse(SocialRealtimePresenceResolver.shouldSkipUserPresence("user-1"))
    }

    @Test
    fun offlineStatus_returnsOfflineFallback() {
        assertEquals(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = false,
                lastChanged = 0L
            ),
            SocialRealtimePresenceResolver.offlineStatus("user-1")
        )
    }

    @Test
    fun resolvePresenceStatus_marksOnlyOnlineStateAsOnline() {
        assertEquals(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = true,
                lastChanged = 123L
            ),
            SocialRealtimePresenceResolver.resolvePresenceStatus(
                userId = "user-1",
                state = "online",
                lastChanged = 123L,
                onlineState = "online"
            )
        )

        assertEquals(
            PresenceStatusUiModel(
                userId = "user-1",
                isOnline = false,
                lastChanged = 0L
            ),
            SocialRealtimePresenceResolver.resolvePresenceStatus(
                userId = "user-1",
                state = "offline",
                lastChanged = null,
                onlineState = "online"
            )
        )
    }

    @Test
    fun shouldSkipTypingUsers_requiresThreadAndCurrentUser() {
        assertTrue(
            SocialRealtimePresenceResolver.shouldSkipTypingUsers(
                threadId = " ",
                currentUserId = "current-user"
            )
        )
        assertTrue(
            SocialRealtimePresenceResolver.shouldSkipTypingUsers(
                threadId = "thread-1",
                currentUserId = " "
            )
        )
        assertFalse(
            SocialRealtimePresenceResolver.shouldSkipTypingUsers(
                threadId = "thread-1",
                currentUserId = "current-user"
            )
        )
    }

    @Test
    fun shouldSkipTypingUpdate_requiresThreadAndUser() {
        assertTrue(
            SocialRealtimePresenceResolver.shouldSkipTypingUpdate(
                threadId = " ",
                userId = "user-1"
            )
        )
        assertTrue(
            SocialRealtimePresenceResolver.shouldSkipTypingUpdate(
                threadId = "thread-1",
                userId = " "
            )
        )
        assertTrue(
            SocialRealtimePresenceResolver.shouldSkipTypingUpdate(
                threadId = "thread-1",
                userId = null
            )
        )
        assertFalse(
            SocialRealtimePresenceResolver.shouldSkipTypingUpdate(
                threadId = "thread-1",
                userId = "user-1"
            )
        )
    }

    @Test
    fun shouldRemoveTyping_returnsTrueOnlyWhenTypingStopped() {
        assertTrue(SocialRealtimePresenceResolver.shouldRemoveTyping(false))
        assertFalse(SocialRealtimePresenceResolver.shouldRemoveTyping(true))
    }
}
