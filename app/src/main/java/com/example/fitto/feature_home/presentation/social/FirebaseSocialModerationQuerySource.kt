package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialModerationQuerySource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SocialModerationQuerySource {

    override suspend fun getBlockedUsers(): List<SocialUser> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()

        val currentUserSnapshot = firestore.collection(USERS_COLLECTION)
            .document(currentUserId)
            .get()
            .await()

        val blockedUserIds = SocialBlockedUserListResolver.blockedUserIds(
            ids = currentUserSnapshot.getStringListCompat(BLOCKED_USER_IDS_FIELD),
            currentUserId = currentUserId
        )

        if (blockedUserIds.isEmpty()) return emptyList()

        val usersById = mutableMapOf<String, SocialUser>()

        SocialBlockedUserListResolver.lookupChunks(blockedUserIds).forEach { chunk ->
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { document ->
                usersById[document.id] = SocialBlockedUserListResolver.resolveBlockedUser(
                    id = document.id,
                    isDeleted = document.getBoolean("isDeleted"),
                    userName = document.getString("userName"),
                    username = document.getString("username"),
                    displayName = document.getString("displayName"),
                    name = document.getString("name"),
                    realName = document.getString("realName"),
                    email = document.getString("email"),
                    photoUrl = document.getString("photoUrl"),
                    bio = document.getString("bio"),
                    followersCount = document.getLong("followersCount"),
                    followingCount = document.getLong("followingCount")
                )
            }
        }

        return SocialBlockedUserListResolver.orderBlockedUsers(
            blockedUserIds = blockedUserIds,
            usersById = usersById
        )
    }

    override suspend fun isUserBlocked(targetUserId: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        if (targetUserId.isBlank()) return false

        val snapshot = currentUserDocument(currentUserId)
            .get()
            .await()

        return SocialDirectThreadBlockStateResolver.isBlocked(
            blockedUserIds = snapshot.get(BLOCKED_USER_IDS_FIELD),
            targetUserId = targetUserId
        )
    }

    override suspend fun getDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): DirectThreadBlockState {
        val currentUserId = auth.currentUser?.uid ?: return SocialDirectThreadBlockStateResolver.empty()
        if (threadId.isBlank() || targetUserId.isBlank()) {
            return SocialDirectThreadBlockStateResolver.empty()
        }

        val snapshot = currentUserDocument(currentUserId)
            .get()
            .await()

        return resolveDirectThreadBlockState(
            snapshot = snapshot,
            threadId = threadId,
            targetUserId = targetUserId
        )
    }

    override fun observeDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): Flow<DirectThreadBlockState> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank() || threadId.isBlank() || targetUserId.isBlank()) {
            trySend(SocialDirectThreadBlockStateResolver.empty())
            close()
            return@callbackFlow
        }

        val listener = currentUserDocument(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(SocialDirectThreadBlockStateResolver.empty())
                    return@addSnapshotListener
                }

                trySend(
                    resolveDirectThreadBlockState(
                        snapshot = snapshot,
                        threadId = threadId,
                        targetUserId = targetUserId
                    )
                )
            }

        awaitClose {
            listener.remove()
        }
    }

    private fun resolveDirectThreadBlockState(
        snapshot: DocumentSnapshot,
        threadId: String,
        targetUserId: String
    ): DirectThreadBlockState {
        return SocialDirectThreadBlockStateResolver.resolve(
            blockedUserIds = snapshot.get(BLOCKED_USER_IDS_FIELD),
            targetUserId = targetUserId,
            completedRanges = SocialMessageHiddenRangeResolver.readCompletedHiddenRanges(
                snapshot = snapshot,
                threadId = threadId
            ),
            activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(
                snapshot = snapshot,
                threadId = threadId
            )
        )
    }

    private fun currentUserDocument(currentUserId: String) =
        firestore.collection(USERS_COLLECTION).document(currentUserId)

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { value -> value as? String }
            .orEmpty()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val BLOCKED_USER_IDS_FIELD = "blockedUserIds"
    }
}
