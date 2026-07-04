package com.example.fitto.feature_home.presentation.social

internal interface SocialPostInteractionSource {
    suspend fun addComment(request: SocialCommentCreateRequest)

    suspend fun toggleCommentLike(postId: String, commentId: String)

    suspend fun toggleSave(postId: String)

    suspend fun toggleLike(postId: String)
}
