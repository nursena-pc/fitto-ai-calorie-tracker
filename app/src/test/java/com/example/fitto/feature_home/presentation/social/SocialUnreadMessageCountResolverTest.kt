package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialUnreadMessageCountResolverTest {

    @Test
    fun unreadCountForThread_returnsUserUnreadCountWhenVisible() {
        val result = SocialUnreadMessageCountResolver.unreadCountForThread(
            userId = "me",
            unreadCounts = mapOf("me" to 3, "other" to 5),
            lastMessageHiddenFor = emptyList(),
            lastMessageCreatedAt = 200L,
            clearedAt = mapOf("me" to 100L)
        )

        assertEquals(3, result)
    }

    @Test
    fun unreadCountForThread_returnsZeroWhenLastMessageHiddenForUser() {
        val result = SocialUnreadMessageCountResolver.unreadCountForThread(
            userId = "me",
            unreadCounts = mapOf("me" to 3),
            lastMessageHiddenFor = listOf("me"),
            lastMessageCreatedAt = 200L,
            clearedAt = emptyMap<String, Long>()
        )

        assertEquals(0, result)
    }

    @Test
    fun unreadCountForThread_returnsZeroWhenThreadWasClearedAfterLastMessage() {
        val result = SocialUnreadMessageCountResolver.unreadCountForThread(
            userId = "me",
            unreadCounts = mapOf("me" to 3),
            lastMessageHiddenFor = emptyList(),
            lastMessageCreatedAt = 200L,
            clearedAt = mapOf("me" to 250L)
        )

        assertEquals(0, result)
    }

    @Test
    fun unreadCountForThread_returnsZeroWhenThreadWasClearedAtLastMessageTime() {
        val result = SocialUnreadMessageCountResolver.unreadCountForThread(
            userId = "me",
            unreadCounts = mapOf("me" to 3),
            lastMessageHiddenFor = emptyList(),
            lastMessageCreatedAt = 200L,
            clearedAt = mapOf("me" to 200L)
        )

        assertEquals(0, result)
    }

    @Test
    fun unreadCountForThread_returnsZeroForMissingUnreadCount() {
        val result = SocialUnreadMessageCountResolver.unreadCountForThread(
            userId = "me",
            unreadCounts = mapOf("other" to 3),
            lastMessageHiddenFor = emptyList(),
            lastMessageCreatedAt = 200L,
            clearedAt = emptyMap<String, Long>()
        )

        assertEquals(0, result)
    }

    @Test
    fun unreadCountForThread_acceptsLongUnreadAndClearedValues() {
        val result = SocialUnreadMessageCountResolver.unreadCountForThread(
            userId = "me",
            unreadCounts = mapOf("me" to 4L),
            lastMessageHiddenFor = emptyList(),
            lastMessageCreatedAt = 300L,
            clearedAt = mapOf("me" to 100)
        )

        assertEquals(4, result)
    }
}
