package com.example.fitto.feature_home.presentation.social

internal class SocialPostMutationCoordinator(
    private val commandSource: SocialPostCommandSource,
    private val interactionSource: SocialPostInteractionSource
) {
    suspend fun createPost(request: SocialPostCreateRequest) {
        commandSource.createPost(request)
    }

    suspend fun createRecipePost(request: SocialRecipePostCreateRequest): String? {
        return commandSource.createRecipePost(request)
    }

    suspend fun addComment(request: SocialCommentCreateRequest) {
        interactionSource.addComment(request)
    }

    suspend fun toggleCommentLike(postId: String, commentId: String) {
        interactionSource.toggleCommentLike(postId, commentId)
    }

    suspend fun toggleSave(postId: String) {
        interactionSource.toggleSave(postId)
    }

    suspend fun toggleLike(postId: String) {
        interactionSource.toggleLike(postId)
    }

    suspend fun updateDraftPost(request: SocialDraftPostUpdateRequest) {
        commandSource.updateDraftPost(request)
    }
}
