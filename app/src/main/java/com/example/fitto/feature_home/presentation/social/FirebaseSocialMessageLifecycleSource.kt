package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialMessageLifecycleSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SocialMessageLifecycleSource {

    override suspend fun clearChatForMe(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank()) return

        val messagesSnapshot = firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .get()
            .await()

        if (messagesSnapshot.isEmpty) return

        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        messagesSnapshot.documents.forEach { document ->
            batch.update(
                document.reference,
                SocialMessageDeletionPayloadResolver.createDeleteForMePayload(
                    currentUserId = currentUserId,
                    updatedAt = now
                )
            )
        }

        batch.update(
            firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId),
            mapOf("updatedAt" to now)
        )

        batch.commit().await()
    }

    override suspend fun markThreadRead(threadId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (!SocialMessageReadStateResolver.canMarkThreadRead(currentUserId, threadId)) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val now = System.currentTimeMillis()

        val threadSnapshot = threadRef.get().await()
        if (!threadSnapshot.exists()) return

        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        val otherUserId = SocialMessageReadStateResolver.directPeerId(
            threadType = threadSnapshot.getString("type"),
            participantIds = participantIds,
            currentUserId = currentUserId
        )

        var isOtherUserCurrentlyBlocked = false
        var hiddenRanges: List<MessageHiddenRange> = emptyList()

        if (!otherUserId.isNullOrBlank()) {
            val userSnapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .get()
                .await()

            val blockedUserIds = SocialMessageReadStateResolver.blockedUserIds(
                userSnapshot.get("blockedUserIds")
            )

            isOtherUserCurrentlyBlocked = SocialMessageReadStateResolver.isPeerBlocked(
                blockedUserIds = blockedUserIds,
                peerId = otherUserId
            )

            hiddenRanges = SocialMessageReadStateResolver.hiddenRanges(
                completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(
                    userSnapshot,
                    threadId
                ),
                activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(
                    userSnapshot,
                    threadId
                )
            )
        }

        val unreadMessages = threadRef
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .get()
            .await()
            .documents
            .filter { document ->
                SocialThreadReadResolver.shouldMarkMessageAsRead(
                    senderId = document.getString("senderId").orEmpty(),
                    createdAt = document.getLong("createdAt") ?: 0L,
                    readBy = document.get("readBy"),
                    currentUserId = currentUserId,
                    otherUserId = otherUserId,
                    isOtherUserCurrentlyBlocked = isOtherUserCurrentlyBlocked,
                    hiddenRanges = hiddenRanges
                )
            }

        firestore.runBatch { batch ->
            batch.update(
                threadRef,
                SocialThreadReadResolver.createThreadReadPayload(currentUserId)
            )

            unreadMessages.forEach { document ->
                batch.set(
                    document.reference,
                    SocialThreadReadResolver.createMessageReadPayload(
                        readBy = document.get("readBy"),
                        currentUserId = currentUserId,
                        readAt = now
                    ),
                    SetOptions.merge()
                )
            }
        }.await()
    }

    override suspend fun deleteMessageForMe(
        threadId: String,
        messageId: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || messageId.isBlank()) return

        val messageRef = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(messageId)

        messageRef.set(
            SocialMessageDeletionPayloadResolver.createDeleteForMePayload(
                currentUserId = currentUserId,
                updatedAt = System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    override suspend fun deleteMessageForEveryone(
        threadId: String,
        messageId: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (threadId.isBlank() || messageId.isBlank()) return

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val messageRef = threadRef.collection(MESSAGES_SUBCOLLECTION).document(messageId)
        val messageSnapshot = messageRef.get().await()

        if (!messageSnapshot.exists()) return
        if (messageSnapshot.getString("senderId") != currentUserId) {
            throw IllegalStateException("ONLY_SENDER_CAN_DELETE_FOR_EVERYONE")
        }

        val now = System.currentTimeMillis()
        val messageCreatedAt = messageSnapshot.getLong("createdAt") ?: 0L
        val threadSnapshot = threadRef.get().await()
        val isLastMessage = threadSnapshot.getLong("lastMessageCreatedAt") == messageCreatedAt

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                SocialMessageDeletionPayloadResolver.createDeleteForEveryonePayload(
                    currentUserId = currentUserId,
                    deletedAt = now
                ),
                SetOptions.merge()
            )

            if (isLastMessage) {
                batch.update(
                    threadRef,
                    SocialMessageDeletionPayloadResolver.createLastMessageDeletedPayload(
                        updatedAt = now
                    )
                )
            }
        }.await()
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
    }
}
