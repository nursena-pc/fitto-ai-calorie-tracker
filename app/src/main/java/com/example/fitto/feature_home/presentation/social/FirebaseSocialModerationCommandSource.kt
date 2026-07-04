package com.example.fitto.feature_home.presentation.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirebaseSocialModerationCommandSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val ensureCurrentUserProfile: suspend () -> Unit
) : SocialModerationCommandSource {

    override suspend fun reportPost(
        postId: String,
        reason: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection(REPORTS_COLLECTION)
            .add(
                SocialModerationPayloadResolver.createPostReportPayload(
                    postId = postId,
                    reporterId = userId,
                    reason = reason,
                    createdAt = System.currentTimeMillis()
                )
            )
            .await()
    }

    override suspend fun blockUser(
        targetUserId: String,
        threadId: String?,
        previewText: String?,
        previewCreatedAt: Long?
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (SocialModerationPayloadResolver.shouldSkipTargetAction(currentUserId, targetUserId)) return

        ensureCurrentUserProfile()

        currentUserDocument(currentUserId)
            .update(
                SocialModerationPayloadResolver.createBlockUserPayload(
                    targetUserId = targetUserId,
                    threadId = threadId,
                    previewText = previewText,
                    previewCreatedAt = previewCreatedAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            .await()
    }

    override suspend fun unblockUser(
        targetUserId: String,
        threadId: String?
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (SocialModerationPayloadResolver.shouldSkipTargetAction(currentUserId, targetUserId)) return

        val userRef = currentUserDocument(currentUserId)
        val snapshot = userRef.get().await()
        val now = System.currentTimeMillis()

        val updates = SocialModerationPayloadResolver.createUnblockBasePayload(
            targetUserId = targetUserId,
            updatedAt = now
        ).toMutableMap()

        fun closeHiddenRangeForThread(targetThreadId: String) {
            updates.putAll(
                SocialModerationPayloadResolver.createCloseBlockedThreadPayload(
                    threadId = targetThreadId,
                    activeRange = SocialMessageHiddenRangeResolver.readActiveHiddenRange(
                        snapshot = snapshot,
                        threadId = targetThreadId
                    ),
                    targetUserId = targetUserId,
                    updatedAt = now
                )
            )
        }

        if (!threadId.isNullOrBlank()) {
            closeHiddenRangeForThread(threadId)
        } else {
            val activeRanges = snapshot.get(ACTIVE_BLOCKED_MESSAGE_RANGES_FIELD) as? Map<*, *>

            activeRanges.orEmpty().forEach { entry ->
                val targetThreadId = entry.key as? String ?: return@forEach
                val activeRange = SocialMessageHiddenRangeResolver.readMessageHiddenRange(entry.value)
                    ?: return@forEach

                if (activeRange.userId == targetUserId) {
                    closeHiddenRangeForThread(targetThreadId)
                }
            }
        }

        userRef.update(updates).await()
    }

    override suspend fun reportUser(
        targetUserId: String,
        threadId: String,
        reason: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (SocialModerationPayloadResolver.shouldSkipTargetAction(currentUserId, targetUserId)) return

        firestore.collection(REPORTS_COLLECTION)
            .add(
                SocialModerationPayloadResolver.createUserReportPayload(
                    targetUserId = targetUserId,
                    reporterId = currentUserId,
                    threadId = threadId,
                    reason = reason,
                    createdAt = System.currentTimeMillis()
                )
            )
            .await()
    }

    private fun currentUserDocument(currentUserId: String) =
        firestore.collection(USERS_COLLECTION).document(currentUserId)

    private companion object {
        const val USERS_COLLECTION = "users"
        const val REPORTS_COLLECTION = "reports"
        const val ACTIVE_BLOCKED_MESSAGE_RANGES_FIELD = "activeBlockedMessageRanges"
    }
}
