package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal interface SocialPostContentObservationSource {
    fun observeComments(postId: String): Flow<List<SocialComment>>

    fun observeMyProfilePosts(): Flow<List<SocialPost>>

    fun observeMyDraftPosts(): Flow<List<SocialPost>>

    fun observeSavedPosts(): Flow<List<SocialPost>>

    fun observePost(postId: String): Flow<SocialPost?>

    suspend fun getPostById(postId: String): SocialPost?
}
