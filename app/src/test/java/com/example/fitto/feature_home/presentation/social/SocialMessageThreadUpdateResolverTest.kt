package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMessageThreadUpdateResolverTest {

    @Test
    fun visibleRecipientIds_excludesSenderAndBlockedRecipients() {
        val recipients = SocialMessageThreadUpdateResolver.visibleRecipientIds(
            participantIds = listOf("sender", "visible-1", "blocked", "visible-2"),
            senderId = "sender",
            blockedRecipientIds = listOf("blocked")
        )

        assertEquals(listOf("visible-1", "visible-2"), recipients)
    }

    @Test
    fun createLastMessageUpdates_setsLastMessageAndUnreadKeys() {
        val updates = SocialMessageThreadUpdateResolver.createLastMessageUpdates(
            lastMessageText = "Merhaba",
            lastMessageSenderId = "sender",
            lastMessageCreatedAt = 100L,
            lastMessageType = "text",
            participantIds = listOf("sender", "visible", "blocked"),
            senderId = "sender",
            blockedRecipientIds = listOf("blocked"),
            updatedAt = 120L
        )

        assertEquals("Merhaba", updates["lastMessageText"])
        assertEquals("sender", updates["lastMessageSenderId"])
        assertEquals(100L, updates["lastMessageCreatedAt"])
        assertEquals("text", updates["lastMessageType"])
        assertEquals(listOf("blocked"), updates["lastMessageHiddenFor"])
        assertEquals(120L, updates["updatedAt"])
        assertEquals(0, updates["unreadCounts.sender"])
        assertEquals(0, updates["unreadCounts.blocked"])
        assertTrue(updates.containsKey("unreadCounts.visible"))
    }
}
