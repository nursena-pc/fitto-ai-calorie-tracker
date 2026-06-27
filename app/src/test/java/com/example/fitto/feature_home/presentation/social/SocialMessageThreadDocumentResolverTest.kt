package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialMessageThreadDocumentResolverTest {

    @Test
    fun resolve_buildsGroupThreadWithDefaults() {
        val result = SocialMessageThreadDocumentResolver.resolve(
            id = "thread-1",
            type = "group",
            currentUserId = "user-1",
            participantIds = listOf("user-1", "user-2"),
            participants = null,
            groupTitle = "",
            groupPhotoUrl = "group-photo",
            lastMessageText = "",
            lastMessageCreatedAt = null,
            updatedAt = null,
            clearedAt = null,
            unreadCounts = null,
            memberCount = null,
            includeClearedThread = false
        )

        requireNotNull(result)
        assertEquals("thread-1", result.id)
        assertEquals("Fitto Grup", result.title)
        assertEquals("Henüz mesaj yok.", result.lastMessage)
        assertEquals("", result.timeText)
        assertEquals(true, result.isGroup)
        assertNull(result.otherParticipantId)
        assertEquals("group-photo", result.photoUrl)
        assertEquals("FG", result.initials)
        assertEquals(2, result.memberCount)
    }

    @Test
    fun resolve_buildsDirectThreadFromOtherParticipant() {
        val result = SocialMessageThreadDocumentResolver.resolve(
            id = "thread-1",
            type = "direct",
            currentUserId = "user-1",
            participantIds = listOf("user-1", "user-2"),
            participants = mapOf(
                "user-2" to mapOf(
                    "userName" to "Ayşe Yılmaz",
                    "photoUrl" to "photo-url"
                )
            ),
            groupTitle = null,
            groupPhotoUrl = null,
            lastMessageText = "Merhaba",
            lastMessageCreatedAt = 0L,
            updatedAt = null,
            clearedAt = null,
            unreadCounts = mapOf("user-1" to 3L),
            memberCount = null,
            includeClearedThread = false
        )

        requireNotNull(result)
        assertEquals("Ayşe Yılmaz", result.title)
        assertEquals("Merhaba", result.lastMessage)
        assertEquals("3 yeni", result.highlightText)
        assertEquals(3, result.unreadCount)
        assertEquals("AY", result.initials)
        assertEquals("user-2", result.otherParticipantId)
        assertEquals("photo-url", result.photoUrl)
    }

    @Test
    fun resolve_hidesClearedThreadWhenNotIncluded() {
        val result = SocialMessageThreadDocumentResolver.resolve(
            id = "thread-1",
            type = "direct",
            currentUserId = "user-1",
            participantIds = listOf("user-1", "user-2"),
            participants = emptyMap<String, Any>(),
            groupTitle = null,
            groupPhotoUrl = null,
            lastMessageText = "Eski mesaj",
            lastMessageCreatedAt = 100L,
            updatedAt = null,
            clearedAt = mapOf("user-1" to 100L),
            unreadCounts = mapOf("user-1" to 5L),
            memberCount = null,
            includeClearedThread = false
        )

        assertNull(result)
    }

    @Test
    fun resolve_resetsClearedThreadWhenIncluded() {
        val result = SocialMessageThreadDocumentResolver.resolve(
            id = "thread-1",
            type = "direct",
            currentUserId = "user-1",
            participantIds = listOf("user-1", "user-2"),
            participants = emptyMap<String, Any>(),
            groupTitle = null,
            groupPhotoUrl = null,
            lastMessageText = "Eski mesaj",
            lastMessageCreatedAt = 100L,
            updatedAt = null,
            clearedAt = mapOf("user-1" to 100L),
            unreadCounts = mapOf("user-1" to 5L),
            memberCount = null,
            includeClearedThread = true
        )

        requireNotNull(result)
        assertEquals("Henüz mesaj yok.", result.lastMessage)
        assertEquals(0L, result.lastMessageCreatedAt)
        assertEquals(0, result.unreadCount)
        assertNull(result.highlightText)
        assertEquals("", result.timeText)
    }
}
