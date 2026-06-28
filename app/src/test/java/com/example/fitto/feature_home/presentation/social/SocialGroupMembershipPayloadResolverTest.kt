package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialGroupMembershipPayloadResolverTest {

    @Test
    fun hasGroupAccess_requiresGroupAndCurrentParticipant() {
        assertTrue(
            SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = true,
                participantIds = listOf("current", "other"),
                currentUserId = "current"
            )
        )

        assertFalse(
            SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = false,
                participantIds = listOf("current"),
                currentUserId = "current"
            )
        )

        assertFalse(
            SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = true,
                participantIds = listOf("other"),
                currentUserId = "current"
            )
        )
    }

    @Test
    fun createRemoveMemberPayload_removesParticipantStateAndUpdatesCount() {
        val payload = SocialGroupMembershipPayloadResolver.createRemoveMemberPayload(
            memberId = "member-1",
            participantIds = listOf("creator", "member-1", "member-2"),
            updatedAt = 100L
        )

        assertTrue(payload["participantIds"] is FieldValue)
        assertTrue(payload["participants.member-1"] is FieldValue)
        assertTrue(payload["unreadCounts.member-1"] is FieldValue)
        assertTrue(payload["typing.member-1"] is FieldValue)
        assertEquals(2, payload["memberCount"])
        assertEquals(100L, payload["updatedAt"])
    }

    @Test
    fun filterNewMembers_removesExistingBlankAndDuplicateUsers() {
        val users = listOf(
            SearchUserUiModel(id = "member-1", userName = "Existing", bio = null, photoUrl = null),
            SearchUserUiModel(id = "", userName = "Blank", bio = null, photoUrl = null),
            SearchUserUiModel(id = "member-2", userName = "New", bio = null, photoUrl = "photo"),
            SearchUserUiModel(id = "member-2", userName = "Duplicate", bio = null, photoUrl = null),
            SearchUserUiModel(id = "member-3", userName = "Other", bio = null, photoUrl = null)
        )

        val result = SocialGroupMembershipPayloadResolver.filterNewMembers(
            selectedUsers = users,
            currentParticipantIds = listOf("member-1")
        )

        assertEquals(listOf("member-2", "member-3"), result.map { it.id })
    }

    @Test
    fun createAddMembersPayload_addsParticipantMapsAndUnreadDefaults() {
        val users = listOf(
            SearchUserUiModel(id = "member-2", userName = "Nursena", bio = null, photoUrl = "photo"),
            SearchUserUiModel(id = "member-3", userName = "Ali", bio = null, photoUrl = null)
        )

        val payload = SocialGroupMembershipPayloadResolver.createAddMembersPayload(
            newUsers = users,
            currentParticipantIds = listOf("member-1"),
            updatedAt = 200L
        )

        assertTrue(payload["participantIds"] is FieldValue)
        assertEquals(3, payload["memberCount"])
        assertEquals(200L, payload["updatedAt"])
        assertEquals(0, payload["unreadCounts.member-2"])
        assertEquals(0, payload["unreadCounts.member-3"])

        @Suppress("UNCHECKED_CAST")
        val participant = payload["participants.member-2"] as Map<String, Any?>
        assertEquals("member-2", participant["uid"])
        assertEquals("Nursena", participant["userName"])
        assertEquals("photo", participant["photoUrl"])
    }
}
