package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialProfileCopyUpdateResolverTest {

    @Test
    fun publicUsernameUpdate_createsPostAndCommentPayload() {
        val result = SocialProfileCopyUpdateResolver.publicUsernameUpdate(
            userName = "nursena",
            now = 123L
        )

        assertEquals("nursena", result["userName"])
        assertEquals(123L, result["updatedAt"])
        assertEquals(2, result.size)
    }

    @Test
    fun participantUsernameUpdate_createsNestedParticipantPayload() {
        val result = SocialProfileCopyUpdateResolver.participantUsernameUpdate(
            uid = "user-1",
            userName = "nursena",
            now = 456L
        )

        assertEquals("nursena", result["participants.user-1.userName"])
        assertEquals(456L, result["updatedAt"])
        assertEquals(2, result.size)
    }

    @Test
    fun senderUsernameUpdate_createsMessagePayload() {
        val result = SocialProfileCopyUpdateResolver.senderUsernameUpdate(
            userName = "nursena",
            now = 789L
        )

        assertEquals("nursena", result["senderName"])
        assertEquals(789L, result["updatedAt"])
        assertEquals(2, result.size)
    }

    @Test
    fun profilePhotoUpdate_setsAllPhotoFieldsWhenPhotoExists() {
        val result = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
            fieldNames = listOf("userPhotoUrl", "photoUrl"),
            photoUrl = "https://example.com/avatar.png",
            now = 111L
        )

        assertEquals("https://example.com/avatar.png", result["userPhotoUrl"])
        assertEquals("https://example.com/avatar.png", result["photoUrl"])
        assertEquals(111L, result["updatedAt"])
        assertEquals(3, result.size)
    }

    @Test
    fun profilePhotoUpdate_deletesAllPhotoFieldsWhenPhotoIsBlank() {
        val result = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
            fieldNames = listOf("userPhotoUrl", "photoUrl"),
            photoUrl = " ",
            now = 222L
        )

        assertTrue(result.containsKey("userPhotoUrl"))
        assertTrue(result.containsKey("photoUrl"))
        assertEquals(222L, result["updatedAt"])
        assertEquals(3, result.size)
    }

    @Test
    fun participantPhotoUpdate_createsNestedParticipantPhotoPayload() {
        val result = SocialProfileCopyUpdateResolver.participantPhotoUpdate(
            uid = "user-1",
            photoUrl = "https://example.com/avatar.png",
            now = 333L
        )

        assertEquals("https://example.com/avatar.png", result["participants.user-1.photoUrl"])
        assertEquals(333L, result["updatedAt"])
        assertEquals(2, result.size)
    }
}
