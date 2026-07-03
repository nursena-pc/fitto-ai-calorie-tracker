package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialThreadObservationSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val loadUsersByIds: suspend (List<String>) -> List<SocialUser>
) : SocialThreadObservationSource {

    override fun observeMessageThreads(): Flow<List<MessageThreadUiModel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        if (SocialMessageThreadListResolver.shouldSkipThreadList(currentUserId)) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val activeUserId = currentUserId.orEmpty()
        var threadDocuments: List<DocumentSnapshot> = emptyList()
        var blockedUserIds: Set<String> = emptySet()
        var blockedThreadPreviews: Map<String, BlockedThreadPreview> = emptyMap()

        fun emitThreads() {
            val threadItems = threadDocuments.mapNotNull { document ->
                val thread = SocialMessageThreadSnapshotMapper.map(
                    document = document,
                    currentUserId = activeUserId
                ) ?: return@mapNotNull null

                SocialMessageThreadListItem(
                    thread = thread,
                    updatedAt = document.getLong("updatedAt") ?: 0L
                )
            }

            val threads = SocialMessageThreadListResolver.resolveThreads(
                items = threadItems,
                blockedUserIds = blockedUserIds,
                blockedThreadPreviews = blockedThreadPreviews
            )

            launch {
                val users = loadUsersByIds(
                    SocialAuthorHydrationResolver.messageThreadParticipantIds(threads)
                )
                trySend(SocialAuthorHydrationResolver.hydrateMessageThreads(threads, users))
            }
        }

        val threadsListener = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .whereArrayContains("participantIds", activeUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                threadDocuments = snapshot?.documents.orEmpty()
                emitThreads()
            }

        val userListener = firestore.collection(USERS_COLLECTION)
            .document(activeUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    blockedUserIds = emptySet()
                    blockedThreadPreviews = emptyMap()
                    emitThreads()
                    return@addSnapshotListener
                }

                blockedUserIds = SocialMessageThreadListResolver.blockedUserIds(
                    snapshot?.get("blockedUserIds")
                )
                blockedThreadPreviews = SocialMessageHiddenRangeResolver.readBlockedThreadPreviews(
                    snapshot?.get("blockedThreadPreviews")
                )
                emitThreads()
            }

        awaitClose {
            threadsListener.remove()
            userListener.remove()
        }
    }

    override fun observeMessages(threadId: String): Flow<List<ChatMessageUiModel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()

        if (SocialMessageListResolver.shouldSkipMessageList(currentUserId, threadId)) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val threadRef = firestore.collection(MESSAGE_THREADS_COLLECTION).document(threadId)
        val threadSnapshot = threadRef.get().await()
        val participantIds = threadSnapshot.getStringListCompat("participantIds")
        val participants = threadSnapshot.get("participants") as? Map<*, *>
        val participantNames = SocialMessageListResolver.participantNames(
            participantIds = participantIds,
            participants = participants
        )
        val latestParticipants = loadUsersByIds(participantIds).associateBy { user -> user.id }
        val participantPhotoUrls = SocialMessageListResolver.participantPhotoUrls(
            participantIds = participantIds,
            participants = participants,
            latestParticipants = latestParticipants
        )
        val totalRecipientCount = SocialMessageListResolver.totalRecipientCount(participantIds)
        val currentUserClearedAt = SocialMessageListResolver.currentUserClearedAt(
            clearedAt = threadSnapshot.get("clearedAt"),
            currentUserId = currentUserId
        )

        val listener = threadRef.collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                    SocialChatMessageDocumentResolver.resolve(
                        id = document.id,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        currentUserId = currentUserId,
                        currentUserClearedAt = currentUserClearedAt,
                        deletedFor = document.getStringListCompat("deletedFor"),
                        senderId = document.getString("senderId").orEmpty(),
                        senderName = document.getString("senderName"),
                        senderPhotoUrl = document.getString("senderPhotoUrl"),
                        fallbackPhotoUrl = document.getString("photoUrl"),
                        participantNames = participantNames,
                        participantPhotoUrls = participantPhotoUrls,
                        readBy = SocialReadReceiptResolver.readByMap(document.get("readBy")),
                        totalRecipientCount = totalRecipientCount,
                        isDeleted = document.getBoolean("isDeleted") == true,
                        messageType = document.getString("type"),
                        text = document.getString("text"),
                        fileName = document.getString("fileName"),
                        fileUrl = document.getString("fileUrl"),
                        fileMimeType = document.getString("fileMimeType"),
                        fileSizeBytes = document.getLong("fileSizeBytes") ?: 0L,
                        audioDurationMillis = document.getLong("audioDurationMillis") ?: 0L,
                        deliveredAt = document.getLong("deliveredAt"),
                        deletedAt = document.getLong("deletedAt") ?: 0L,
                        deletedBy = document.getString("deletedBy")
                    )
                }

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    override fun observeThreadMessagesForSearch(
        threadId: String
    ): Flow<List<GroupMessageSearchResultUiModel>> = callbackFlow {
        if (SocialGroupMessageSearchResolver.shouldSkipSearch(threadId)) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { document ->
                    SocialGroupMessageSearchResolver.resolveResult(
                        messageId = document.id,
                        threadId = threadId,
                        text = document.getString("text"),
                        senderName = document.getString("senderName"),
                        senderDisplayName = document.getString("senderDisplayName"),
                        createdAt = document.getLong("createdAt")
                    )
                }

                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    override fun observeThreadSharedContentSummary(
        threadId: String
    ): Flow<ThreadSharedContentSummary> = callbackFlow {
        val registration = firestore
            .collection(MESSAGE_THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().map { document ->
                    SocialThreadSharedContentSummaryResolver.Message(
                        text = document.getString("text").orEmpty(),
                        content = document.getString("content").orEmpty(),
                        attachmentUrl = document.getString("attachmentUrl"),
                        attachmentType = document.getString("attachmentType").orEmpty(),
                        mimeType = document.getString("mimeType").orEmpty(),
                        imageUrl = document.getString("imageUrl"),
                        videoUrl = document.getString("videoUrl"),
                        linkUrl = document.getString("linkUrl")
                    )
                }

                trySend(SocialThreadSharedContentSummaryResolver.summarize(messages))
            }

        awaitClose { registration.remove() }
    }

    override fun observeUnreadMessageCount(): Flow<Int> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null

        fun attachForUser(userId: String) {
            listenerRegistration?.remove()
            listenerRegistration = null

            if (userId.isBlank()) {
                trySend(0)
                return
            }

            listenerRegistration = firestore.collection(MESSAGE_THREADS_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(0)
                        return@addSnapshotListener
                    }

                    val totalUnread = snapshot?.documents.orEmpty().sumOf { document ->
                        SocialUnreadMessageCountResolver.unreadCountForThread(
                            userId = userId,
                            unreadCounts = document.get("unreadCounts"),
                            lastMessageHiddenFor = document.getStringListCompat("lastMessageHiddenFor"),
                            lastMessageCreatedAt = document.getLong("lastMessageCreatedAt") ?: 0L,
                            clearedAt = document.get("clearedAt")
                        )
                    }

                    trySend(totalUnread)
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            attachForUser(firebaseAuth.currentUser?.uid.orEmpty())
        }

        auth.addAuthStateListener(authListener)
        attachForUser(auth.currentUser?.uid.orEmpty())

        awaitClose {
            listenerRegistration?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { value -> value as? String }
            .orEmpty()
    }

    private companion object {
        const val MESSAGE_THREADS_COLLECTION = "messageThreads"
        const val MESSAGES_SUBCOLLECTION = "messages"
        const val USERS_COLLECTION = "users"
    }
}
