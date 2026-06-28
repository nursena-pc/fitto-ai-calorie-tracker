package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialGroupMemberResolverTest {

    @Test
    fun resolveMembers_mapsParticipantProfilesAndFallbacks() {
        val members = SocialGroupMemberResolver.resolveMembers(
            participantIds = listOf("creator", "me", "empty-name", "missing"),
            participants = mapOf(
                "creator" to mapOf(
                    "userName" to "zeynep",
                    "photoUrl" to "creator-photo"
                ),
                "me" to mapOf(
                    "userName" to "nursena",
                    "photoUrl" to "me-photo"
                ),
                "empty-name" to mapOf(
                    "userName" to "   ",
                    "photoUrl" to "empty-photo"
                )
            ),
            currentUserId = "me",
            creatorId = "creator"
        )

        assertEquals(
            listOf("me", "creator", "empty-name", "missing"),
            members.map { it.uid }
        )
        assertEquals("nursena", members[0].userName)
        assertEquals("me-photo", members[0].photoUrl)
        assertEquals(true, members[0].isMe)
        assertEquals(false, members[0].isCreator)
        assertEquals("fitto_user", members[2].userName)
        assertEquals("empty-photo", members[2].photoUrl)
        assertEquals("fitto_user", members[3].userName)
        assertEquals(null, members[3].photoUrl)
    }

    @Test
    fun resolveMembers_sortsCurrentUserThenCreatorThenName() {
        val members = SocialGroupMemberResolver.resolveMembers(
            participantIds = listOf("b", "creator", "me", "a"),
            participants = mapOf(
                "b" to mapOf("userName" to "zeynep"),
                "creator" to mapOf("userName" to "ayşe"),
                "me" to mapOf("userName" to "nursena"),
                "a" to mapOf("userName" to "ali")
            ),
            currentUserId = "me",
            creatorId = "creator"
        )

        assertEquals(
            listOf("me", "creator", "a", "b"),
            members.map { it.uid }
        )
    }
}
