package com.example.fitto.feature_home.presentation.social

internal interface SocialModerationCommandSource {
    suspend fun reportPost(
        postId: String,
        reason: String
    )

    suspend fun blockUser(
        targetUserId: String,
        threadId: String?,
        previewText: String?,
        previewCreatedAt: Long?
    )

    suspend fun unblockUser(
        targetUserId: String,
        threadId: String?
    )

    suspend fun reportUser(
        targetUserId: String,
        threadId: String,
        reason: String
    )
}
