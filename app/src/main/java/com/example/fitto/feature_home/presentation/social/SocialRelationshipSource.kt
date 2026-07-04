package com.example.fitto.feature_home.presentation.social

internal interface SocialRelationshipSource {
    suspend fun toggleFollow(targetUser: SocialUser)

    suspend fun removeFollower(followerId: String)

    suspend fun stopFollowing(followingId: String)

    suspend fun getFollowers(userId: String): List<SocialUser>

    suspend fun getFollowing(userId: String): List<SocialUser>
}
