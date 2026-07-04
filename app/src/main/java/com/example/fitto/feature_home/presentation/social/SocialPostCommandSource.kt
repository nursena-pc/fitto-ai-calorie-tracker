package com.example.fitto.feature_home.presentation.social

internal interface SocialPostCommandSource {
    suspend fun createPost(request: SocialPostCreateRequest)

    suspend fun createRecipePost(request: SocialRecipePostCreateRequest): String?

    suspend fun updateDraftPost(request: SocialDraftPostUpdateRequest)
}
