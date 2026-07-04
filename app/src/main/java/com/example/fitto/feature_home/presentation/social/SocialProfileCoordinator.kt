package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri

internal class SocialProfileCoordinator(
    private val querySource: SocialProfileQuerySource,
    private val commandSource: SocialProfileCommandSource,
    private val relationshipSource: SocialRelationshipSource
) {
    suspend fun searchUsers(query: String): List<SearchUserUiModel> {
        return querySource.searchUsers(query)
    }

    suspend fun createCurrentUserProfileIfNeeded() {
        querySource.createCurrentUserProfileIfNeeded()
    }

    suspend fun getCurrentUserProfile(): SocialUser? {
        return querySource.getCurrentUserProfile()
    }

    suspend fun getUserProfile(userId: String): SocialUser? {
        return querySource.getUserProfile(userId)
    }

    suspend fun isUserDeletedOrMissing(userId: String): Boolean {
        return querySource.isUserDeletedOrMissing(userId)
    }

    suspend fun getUsersByIds(userIds: List<String>): List<SocialUser> {
        return querySource.getUsersByIds(userIds)
    }

    suspend fun updateCurrentUserProfile(displayName: String, bio: String) {
        commandSource.updateCurrentUserProfile(displayName, bio)
    }

    suspend fun updateCurrentUserPhotoUrl(photoUrl: String?) {
        commandSource.updateCurrentUserPhotoUrl(photoUrl)
    }

    suspend fun uploadCurrentUserProfilePhoto(context: Context, imageUri: Uri): String {
        return commandSource.uploadCurrentUserProfilePhoto(context, imageUri)
    }

    suspend fun toggleFollow(targetUser: SocialUser) {
        relationshipSource.toggleFollow(targetUser)
    }

    suspend fun removeFollower(followerId: String) {
        relationshipSource.removeFollower(followerId)
    }

    suspend fun stopFollowing(followingId: String) {
        relationshipSource.stopFollowing(followingId)
    }

    suspend fun getFollowers(userId: String): List<SocialUser> {
        return relationshipSource.getFollowers(userId)
    }

    suspend fun getFollowing(userId: String): List<SocialUser> {
        return relationshipSource.getFollowing(userId)
    }
}
