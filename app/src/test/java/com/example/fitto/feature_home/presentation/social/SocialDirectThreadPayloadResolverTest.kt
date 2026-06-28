package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialDirectThreadPayloadResolverTest {

    @Test
    fun canCreateDirectThread_rejectsBlankOrSelfTarget() {
        assertFalse(
            SocialDirectThreadPayloadResolver.canCreateDirectThread(
                currentUserId = "current",
                targetUserId = ""
            )
        )
        assertFalse(
            SocialDirectThreadPayloadResolver.canCreateDirectThread(
                currentUserId = "same",
                targetUserId = "same"
            )
        )
        assertFalse(
            SocialDirectThreadPayloadResolver.canCreateDirectThread(
                currentUserId = "",
                targetUserId = "target"
            )
        )
        assertTrue(
            SocialDirectThreadPayloadResolver.canCreateDirectThread(
                currentUserId = "current",
                targetUserId = "target"
            )
        )
    }

    @Test
    fun currentUserName_prefersProfileNameThenEmailPrefixThenDefault() {
        assertEquals(
            "profile_user",
            SocialDirectThreadPayloadResolver.currentUserName(
                profileUserName = "profile_user",
                email = "mail@example.com"
            )
        )
        assertEquals(
            "mail",
            SocialDirectThreadPayloadResolver.currentUserName(
                profileUserName = "",
                email = "mail@example.com"
            )
        )
        assertEquals(
            "fitto_user",
            SocialDirectThreadPayloadResolver.currentUserName(
                profileUserName = null,
                email = null
            )
        )
    }

    @Test
    fun targetUserName_fallsBackToDefaultWhenBlank() {
        assertEquals("target_user", SocialDirectThreadPayloadResolver.targetUserName("target_user"))
        assertEquals("fitto_user", SocialDirectThreadPayloadResolver.targetUserName(""))
        assertEquals("fitto_user", SocialDirectThreadPayloadResolver.targetUserName(null))
    }

    @Test
    fun createPayload_buildsDirectThreadPayload() {
        val payload = SocialDirectThreadPayloadResolver.createPayload(
            threadId = "thread-1",
            currentUserId = "user-b",
            targetUserId = "user-a",
            currentUserName = "current",
            currentUserPhotoUrl = "current-photo",
            targetUserName = "target",
            targetUserPhotoUrl = "target-photo",
            now = 123L,
            participantMapFactory = { uid, userName, photoUrl ->
                mapOf(
                    "uid" to uid,
                    "userName" to userName,
                    "photoUrl" to photoUrl
                )
            }
        )

        assertEquals("thread-1", payload["id"])
        assertEquals("direct", payload["type"])
        assertNull(payload["title"])
        assertEquals(listOf("user-a", "user-b"), payload["participantIds"])
        assertEquals(2, payload["memberCount"])
        assertEquals("", payload["lastMessageText"])
        assertNull(payload["lastMessageSenderId"])
        assertNull(payload["lastMessageCreatedAt"])
        assertEquals(123L, payload["createdAt"])
        assertEquals(123L, payload["deliveredAt"])
        assertEquals(123L, payload["updatedAt"])

        @Suppress("UNCHECKED_CAST")
        val participants = payload["participants"] as Map<String, Map<String, Any?>>
        assertEquals("current", participants.getValue("user-b")["userName"])
        assertEquals("current-photo", participants.getValue("user-b")["photoUrl"])
        assertEquals("target", participants.getValue("user-a")["userName"])
        assertEquals("target-photo", participants.getValue("user-a")["photoUrl"])

        assertEquals(
            mapOf("user-b" to 0, "user-a" to 0),
            payload["unreadCounts"]
        )
    }
}
