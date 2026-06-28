package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue

internal object SocialGroupMembershipPayloadResolver {

    fun hasGroupAccess(
        isGroup: Boolean,
        participantIds: List<String>,
        currentUserId: String
    ): Boolean {
        return isGroup && participantIds.contains(currentUserId)
    }

    fun createRemoveMemberPayload(
        memberId: String,
        participantIds: List<String>,
        updatedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "participantIds" to FieldValue.arrayRemove(memberId),
            "participants.$memberId" to FieldValue.delete(),
            "unreadCounts.$memberId" to FieldValue.delete(),
            "typing.$memberId" to FieldValue.delete(),
            "memberCount" to (participantIds.size - 1).coerceAtLeast(0),
            "updatedAt" to updatedAt
        )
    }

    fun filterNewMembers(
        selectedUsers: List<SearchUserUiModel>,
        currentParticipantIds: List<String>
    ): List<SearchUserUiModel> {
        return selectedUsers
            .filter { user -> user.id.isNotBlank() && !currentParticipantIds.contains(user.id) }
            .distinctBy { user -> user.id }
    }

    fun createAddMembersPayload(
        newUsers: List<SearchUserUiModel>,
        currentParticipantIds: List<String>,
        updatedAt: Long
    ): Map<String, Any> {
        val updates = mutableMapOf<String, Any>(
            "participantIds" to FieldValue.arrayUnion(*newUsers.map { it.id }.toTypedArray()),
            "memberCount" to currentParticipantIds.size + newUsers.size,
            "updatedAt" to updatedAt
        )

        newUsers.forEach { user ->
            updates["participants.${user.id}"] = SocialThreadParticipantResolver.participantMap(
                uid = user.id,
                userName = user.userName,
                photoUrl = user.photoUrl
            )
            updates["unreadCounts.${user.id}"] = 0
        }

        return updates
    }
}
