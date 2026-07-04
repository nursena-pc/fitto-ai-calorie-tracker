package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialGroupMembershipSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SocialGroupMembershipSource {

    override suspend fun leaveGroup(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val participantIds = snapshot.getStringListCompat("participantIds")
        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP

        if (!SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = isGroup,
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        threadRef.update(
            SocialGroupMembershipPayloadResolver.createRemoveMemberPayload(
                memberId = currentUserId,
                participantIds = participantIds,
                updatedAt = System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun removeMemberFromGroup(
        threadId: String,
        memberId: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || memberId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP
        val participantIds = snapshot.getStringListCompat("participantIds")
        val creatorId = snapshot.getString("creatorId").orEmpty()

        if (!SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = isGroup,
                participantIds = participantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        if (currentUserId != creatorId) {
            throw IllegalStateException("ONLY_CREATOR_CAN_REMOVE_MEMBER")
        }

        if (memberId == creatorId) {
            throw IllegalStateException("CREATOR_CAN_NOT_BE_REMOVED")
        }

        if (!participantIds.contains(memberId)) return

        threadRef.update(
            SocialGroupMembershipPayloadResolver.createRemoveMemberPayload(
                memberId = memberId,
                participantIds = participantIds,
                updatedAt = System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun addMembersToGroup(
        threadId: String,
        selectedUsers: List<SearchUserUiModel>
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || selectedUsers.isEmpty()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val snapshot = threadRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("THREAD_NOT_FOUND")
        }

        val isGroup = snapshot.getString("type") == THREAD_TYPE_GROUP
        val currentParticipantIds = snapshot.getStringListCompat("participantIds")

        if (!SocialGroupMembershipPayloadResolver.hasGroupAccess(
                isGroup = isGroup,
                participantIds = currentParticipantIds,
                currentUserId = currentUserId
            )
        ) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val newUsers = SocialGroupMembershipPayloadResolver.filterNewMembers(
            selectedUsers = selectedUsers,
            currentParticipantIds = currentParticipantIds
        )

        if (newUsers.isEmpty()) return

        threadRef.update(
            SocialGroupMembershipPayloadResolver.createAddMembersPayload(
                newUsers = newUsers,
                currentParticipantIds = currentParticipantIds,
                updatedAt = System.currentTimeMillis()
            )
        ).await()
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private companion object {
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val THREAD_TYPE_GROUP = "group"
    }
}
