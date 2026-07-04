package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialThreadQuerySource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val searchUsers: suspend (String) -> List<SearchUserUiModel>
) : SocialThreadQuerySource {

    override suspend fun getGroupSettings(threadId: String): GroupSettingsUiModel {
        if (threadId.isBlank()) return GroupSettingsUiModel()

        val snapshot = firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        return SocialGroupSettingsResolver.fromRawMap(
            snapshot.get("settings") as? Map<*, *>
        )
    }

    override suspend fun getGroupMembers(threadId: String): List<GroupMemberUiModel> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        if (threadId.isBlank()) return emptyList()

        val threadSnapshot = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        if (!threadSnapshot.exists()) return emptyList()

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        return SocialGroupMemberResolver.resolveMembers(
            participantIds = participantIds,
            participants = threadSnapshot.get("participants") as? Map<*, *>,
            currentUserId = currentUserId,
            creatorId = threadSnapshot.getString("creatorId").orEmpty()
        )
    }

    override suspend fun getThreadSharedContent(threadId: String): List<SharedContentUiModel> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        if (threadId.isBlank()) return emptyList()

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()

        if (!threadSnapshot.exists()) return emptyList()

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        val messagesSnapshot = threadRef
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return messagesSnapshot.documents.flatMap { document ->
            SocialThreadSharedContentResolver.resolve(
                messageId = document.id,
                text = document.getString("text").orEmpty(),
                createdAt = document.getLong("createdAt") ?: 0L,
                senderName = document.getString("senderName").orEmpty(),
                imageUrl = document.getString("imageUrl")
                    ?: document.getString("mediaUrl")
                    ?: document.getString("photoUrl"),
                fileUrl = document.getString("fileUrl"),
                fileName = document.getString("fileName").orEmpty(),
                linkExtractor = SocialLinkResolver::extractLinks
            )
        }
    }

    override suspend fun searchUsersForGroupInvite(
        threadId: String,
        query: String
    ): List<SearchUserUiModel> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        if (threadId.isBlank()) return emptyList()

        val threadSnapshot = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        val participantIds = threadSnapshot.getStringListCompat("participantIds")

        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        return searchUsers(query)
            .filterNot { user -> participantIds.contains(user.id) }
    }

    override suspend fun getMessageThread(threadId: String): MessageThreadUiModel? {
        val currentUserId = auth.currentUser?.uid ?: return null
        if (threadId.isBlank()) return null

        return SocialMessageThreadSnapshotMapper.map(
            document = firestore.collection(MESSAGE_THREADS_COLLECTION)
                .document(threadId)
                .get()
                .await(),
            currentUserId = currentUserId
        )
    }

    override suspend fun getMessageThreadRawData(threadId: String): Map<String, Any?> {
        val currentUserId = auth.currentUser?.uid ?: return emptyMap()
        if (threadId.isBlank()) return emptyMap()

        val snapshot = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .get()
            .await()

        if (!snapshot.exists()) return emptyMap()

        val participantIds = snapshot.getStringListCompat("participantIds")
        if (!participantIds.contains(currentUserId)) {
            throw IllegalStateException("THREAD_ACCESS_DENIED")
        }

        return snapshot.data.orEmpty()
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private companion object {
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
    }
}
