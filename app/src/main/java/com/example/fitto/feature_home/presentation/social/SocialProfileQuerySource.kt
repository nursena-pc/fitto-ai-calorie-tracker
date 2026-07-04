package com.example.fitto.feature_home.presentation.social

internal interface SocialProfileQuerySource {
    suspend fun searchUsers(query: String): List<SearchUserUiModel>

    suspend fun createCurrentUserProfileIfNeeded()

    suspend fun getCurrentUserProfile(): SocialUser?

    suspend fun getUserProfile(userId: String): SocialUser?

    suspend fun isUserDeletedOrMissing(userId: String): Boolean

    suspend fun getUsersByIds(userIds: List<String>): List<SocialUser>
}
