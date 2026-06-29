package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialDirectThreadPayloadResolverTest {

    @Test
    fun shouldSkipTarget_returnsTrueForBlankOrSelfTarget() {
        assertTrue(
            SocialDirectThreadPayloadResolver.shouldSkipTarget(
                currentUserId = "current",
                targetUserId = " "
            )
        )
        assertTrue(
            SocialDirectThreadPayloadResolver.shouldSkipTarget(
                currentUserId = "current",
                targetUserId = "current"
            )
        )
        assertFalse(
            SocialDirectThreadPayloadResolver.shouldSkipTarget(
                currentUserId = "current",
                targetUserId = "target"
            )
        )
    }

    @Test
    fun currentParticipant_prefersProfileNameAndProfilePhoto() {
        val participant = SocialDirectThreadPayloadResolver.currentParticipant(
            uid = "current",
            profileUserName = "nursena",
            profilePhotoUrl = "profile-photo",
            email = "mail@example.com",
            authPhotoUrl = "auth-photo"
        )

        assertEquals("current", participant.uid)
        assertEquals("nursena", participant.userName)
        assertEquals("profile-photo", participant.photoUrl)
    }

    @Test
    fun currentParticipant_fallsBackToEmailNameAndAuthPhoto() {
        val participant = SocialDirectThreadPayloadResolver.currentParticipant(
            uid = "current",
            profileUserName = " ",
            profilePhotoUrl = null,
            email = "nursena@example.com",
            authPhotoUrl = "auth-photo"
        )

        assertEquals("nursena", participant.userName)
        assertEquals("auth-photo", participant.photoUrl)
    }

    @Test
    fun currentParticipant_fallsBackToDefaultUserNameWithoutEmail() {
        val participant = SocialDirectThreadPayloadResolver.currentParticipant(
            uid = "current",
            profileUserName = null,
            profilePhotoUrl = null,
            email = null,
            authPhotoUrl = null
        )

        assertEquals("fitto_user", participant.userName)
        assertNull(participant.photoUrl)
    }

    @Test
    fun targetParticipant_fallsBackToDefaultUserName() {
        val participant = SocialDirectThreadPayloadResolver.targetParticipant(
            uid = "target",
            userName = " ",
            photoUrl = "target-photo"
        )

        assertEquals("target", participant.uid)
        assertEquals("fitto_user", participant.userName)
        assertEquals("target-photo", participant.photoUrl)
    }

    @Test
    fun createPayload_buildsDirectThreadPayload() {
        val payload = SocialDirectThreadPayloadResolver.createPayload(
            threadId = "direct_current_target",
            threadType = "direct",
            currentParticipant = SocialDirectThreadParticipant(
                uid = "current",
                userName = "nursena",
                photoUrl = "current-photo"
            ),
            targetParticipant = SocialDirectThreadParticipant(
                uid = "target",
                userName = "ayse",
                photoUrl = null
            ),
            now = 123L
        )

        assertEquals("direct_current_target", payload["id"])
        assertEquals("direct", payload["type"])
        assertNull(payload["title"])
        assertEquals(listOf("current", "target"), payload["participantIds"])
        assertEquals(2, payload["memberCount"])
        assertEquals(mapOf("current" to 0, "target" to 0), payload["unreadCounts"])
        assertEquals("", payload["lastMessageText"])
        assertNull(payload["lastMessageSenderId"])
        assertNull(payload["lastMessageCreatedAt"])
        assertEquals(123L, payload["createdAt"])
        assertEquals(123L, payload["deliveredAt"])
        assertEquals(123L, payload["updatedAt"])

        @Suppress("UNCHECKED_CAST")
        val participants = payload["participants"] as Map<String, Map<String, Any?>>
        assertEquals("nursena", participants.getValue("current")["userName"])
        assertEquals("current-photo", participants.getValue("current")["photoUrl"])
        assertEquals("ayse", participants.getValue("target")["userName"])
        assertNull(participants.getValue("target")["photoUrl"])
    }
}
