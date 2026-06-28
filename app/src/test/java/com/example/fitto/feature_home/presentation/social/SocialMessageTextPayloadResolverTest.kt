package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialMessageTextPayloadResolverTest {

    @Test
    fun createTextMessagePayload_mapsMessageFields() {
        val payload = SocialMessageTextPayloadResolver.createTextMessagePayload(
            messageId = "message-1",
            threadId = "thread-1",
            senderId = "sender-1",
            sender = SocialMessageSender(
                name = "nursena",
                photoUrl = "photo-url"
            ),
            text = "merhaba",
            deletedFor = listOf("blocked-1"),
            readAt = 123L
        )

        assertEquals("message-1", payload["id"])
        assertEquals("thread-1", payload["threadId"])
        assertEquals("sender-1", payload["senderId"])
        assertEquals("nursena", payload["senderName"])
        assertEquals("photo-url", payload["senderPhotoUrl"])
        assertEquals("photo-url", payload["photoUrl"])
        assertEquals("merhaba", payload["text"])
        assertEquals("text", payload["type"])
        assertEquals(mapOf("sender-1" to 123L), payload["readBy"])
        assertEquals(listOf("blocked-1"), payload["deletedFor"])
        assertEquals(123L, payload["createdAt"])
        assertEquals(123L, payload["deliveredAt"])
    }

    @Test
    fun createTextMessagePayload_allowsMissingPhoto() {
        val payload = SocialMessageTextPayloadResolver.createTextMessagePayload(
            messageId = "message-1",
            threadId = "thread-1",
            senderId = "sender-1",
            sender = SocialMessageSender(
                name = "nursena",
                photoUrl = null
            ),
            text = "merhaba",
            deletedFor = emptyList(),
            readAt = 456L
        )

        assertEquals(null, payload["senderPhotoUrl"])
        assertEquals(null, payload["photoUrl"])
        assertEquals(emptyList<String>(), payload["deletedFor"])
    }
}
