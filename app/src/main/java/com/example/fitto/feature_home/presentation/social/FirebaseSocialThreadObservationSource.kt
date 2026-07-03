package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class FirebaseSocialThreadObservationSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SocialThreadObservationSource {

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
    }
}
