package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialThreadReadResolverTest {

    @Test
    fun shouldMarkMessageAsRead_returnsFalseForCurrentUserMessage() {
        val result = SocialThreadReadResolver.shouldMarkMessageAsRead(
            senderId = "me",
            createdAt = 100L,
            readBy = emptyList<String>(),
            currentUserId = "me",
            otherUserId = "other",
            isOtherUserCurrentlyBlocked = false,
            hiddenRanges = emptyList()
        )

        assertFalse(result)
    }

    @Test
    fun shouldMarkMessageAsRead_returnsFalseWhenAlreadyRead() {
        val result = SocialThreadReadResolver.shouldMarkMessageAsRead(
            senderId = "other",
            createdAt = 100L,
            readBy = listOf("me"),
            currentUserId = "me",
            otherUserId = "other",
            isOtherUserCurrentlyBlocked = false,
            hiddenRanges = emptyList()
        )

        assertFalse(result)
    }

    @Test
    fun shouldMarkMessageAsRead_returnsFalseForMessageInsideHiddenRange() {
        val result = SocialThreadReadResolver.shouldMarkMessageAsRead(
            senderId = "other",
            createdAt = 150L,
            readBy = emptyList<String>(),
            currentUserId = "me",
            otherUserId = "other",
            isOtherUserCurrentlyBlocked = false,
            hiddenRanges = listOf(
                MessageHiddenRange(
                    userId = "other",
                    startAt = 100L,
                    endAt = 200L
                )
            )
        )

        assertFalse(result)
    }

    @Test
    fun shouldMarkMessageAsRead_returnsFalseForCurrentlyBlockedSenderWithoutRanges() {
        val result = SocialThreadReadResolver.shouldMarkMessageAsRead(
            senderId = "other",
            createdAt = 150L,
            readBy = emptyList<String>(),
            currentUserId = "me",
            otherUserId = "other",
            isOtherUserCurrentlyBlocked = true,
            hiddenRanges = emptyList()
        )

        assertFalse(result)
    }

    @Test
    fun shouldMarkMessageAsRead_returnsTrueForVisibleUnreadMessage() {
        val result = SocialThreadReadResolver.shouldMarkMessageAsRead(
            senderId = "other",
            createdAt = 250L,
            readBy = mapOf("other" to 100L),
            currentUserId = "me",
            otherUserId = "other",
            isOtherUserCurrentlyBlocked = false,
            hiddenRanges = listOf(
                MessageHiddenRange(
                    userId = "other",
                    startAt = 100L,
                    endAt = 200L
                )
            )
        )

        assertTrue(result)
    }

    @Test
    fun createThreadReadPayload_resetsCurrentUserUnreadCount() {
        assertEquals(
            mapOf("unreadCounts.me" to 0),
            SocialThreadReadResolver.createThreadReadPayload("me")
        )
    }

    @Test
    fun createMessageReadPayload_addsCurrentUserReadTimestamp() {
        val payload = SocialThreadReadResolver.createMessageReadPayload(
            readBy = mapOf("other" to 100L),
            currentUserId = "me",
            readAt = 250L
        )

        assertEquals(
            mapOf(
                "other" to 100L,
                "me" to 250L
            ),
            payload["readBy"]
        )
        assertEquals(250L, payload["updatedAt"])
    }
}
