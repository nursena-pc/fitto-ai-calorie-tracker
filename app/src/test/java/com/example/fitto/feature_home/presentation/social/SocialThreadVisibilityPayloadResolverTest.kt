package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialThreadVisibilityPayloadResolverTest {

    @Test
    fun isParticipant_returnsTrueWhenCurrentUserIsInParticipants() {
        assertTrue(
            SocialThreadVisibilityPayloadResolver.isParticipant(
                participantIds = listOf("user-1", "user-2"),
                currentUserId = "user-1"
            )
        )
    }

    @Test
    fun isParticipant_returnsFalseWhenCurrentUserIsBlankOrMissing() {
        assertFalse(
            SocialThreadVisibilityPayloadResolver.isParticipant(
                participantIds = listOf("user-1"),
                currentUserId = ""
            )
        )

        assertFalse(
            SocialThreadVisibilityPayloadResolver.isParticipant(
                participantIds = listOf("user-1"),
                currentUserId = "user-2"
            )
        )
    }

    @Test
    fun createClearPayload_resetsCurrentUserThreadVisibility() {
        assertEquals(
            mapOf(
                "clearedAt.user-1" to 123L,
                "unreadCounts.user-1" to 0,
                "updatedAt" to 123L
            ),
            SocialThreadVisibilityPayloadResolver.createClearPayload(
                currentUserId = "user-1",
                clearedAt = 123L
            )
        )
    }
}
