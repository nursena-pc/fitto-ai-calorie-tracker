package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal class SocialModerationCoordinator(
    private val querySource: SocialModerationQuerySource,
    private val commandSource: SocialModerationCommandSource
) {
    suspend fun getBlockedUsers(): List<SocialUser> {
        return querySource.getBlockedUsers()
    }

    suspend fun isUserBlocked(targetUserId: String): Boolean {
        return querySource.isUserBlocked(targetUserId)
    }

    suspend fun getDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): DirectThreadBlockState {
        return querySource.getDirectThreadBlockState(threadId, targetUserId)
    }

    fun observeDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): Flow<DirectThreadBlockState> {
        return querySource.observeDirectThreadBlockState(threadId, targetUserId)
    }

    suspend fun reportPost(
        postId: String,
        reason: String
    ) {
        commandSource.reportPost(postId, reason)
    }

    suspend fun blockUser(
        targetUserId: String,
        threadId: String?,
        previewText: String?,
        previewCreatedAt: Long?
    ) {
        commandSource.blockUser(
            targetUserId = targetUserId,
            threadId = threadId,
            previewText = previewText,
            previewCreatedAt = previewCreatedAt
        )
    }

    suspend fun unblockUser(
        targetUserId: String,
        threadId: String?
    ) {
        commandSource.unblockUser(targetUserId, threadId)
    }

    suspend fun reportUser(
        targetUserId: String,
        threadId: String,
        reason: String
    ) {
        commandSource.reportUser(targetUserId, threadId, reason)
    }
}
