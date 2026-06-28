package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SocialMessageDeletionPayloadResolverTest {

    @Test
    fun createDeleteForMePayload_marksCurrentUserAndUpdatedAt() {
        val payload = SocialMessageDeletionPayloadResolver.createDeleteForMePayload(
            currentUserId = "user-1",
            updatedAt = 100L
        )

        assertNotNull(payload["deletedFor"])
        assertEquals(100L, payload["updatedAt"])
    }

    @Test
    fun createDeleteForEveryonePayload_clearsTextAndMarksDeleted() {
        val payload = SocialMessageDeletionPayloadResolver.createDeleteForEveryonePayload(
            currentUserId = "user-1",
            deletedAt = 200L
        )

        assertEquals("", payload["text"])
        assertEquals(true, payload["isDeleted"])
        assertEquals("user-1", payload["deletedBy"])
        assertEquals(200L, payload["deletedAt"])
        assertEquals(200L, payload["updatedAt"])
    }

    @Test
    fun createLastMessageDeletedPayload_updatesThreadPreview() {
        val payload = SocialMessageDeletionPayloadResolver.createLastMessageDeletedPayload(
            updatedAt = 300L
        )

        assertEquals("Bu mesaj silindi", payload["lastMessageText"])
        assertEquals(300L, payload["updatedAt"])
    }
}
