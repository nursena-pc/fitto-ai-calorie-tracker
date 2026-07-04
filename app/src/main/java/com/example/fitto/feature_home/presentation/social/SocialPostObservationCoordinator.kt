package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal class SocialPostObservationCoordinator(
    private val feedSource: SocialFeedObservationSource,
    private val contentSource: SocialPostContentObservationSource
) {
    fun observeFeed(): Flow<List<SocialPost>> {
        return feedSource.observeFeed()
    }

    fun observeComments(postId: String): Flow<List<SocialComment>> {
        return contentSource.observeComments(postId)
    }

    suspend fun refreshFeedFromServer() {
        feedSource.refreshFeedFromServer()
    }

    fun observeMyProfilePosts(): Flow<List<SocialPost>> {
        return contentSource.observeMyProfilePosts()
    }

    fun observeMyDraftPosts(): Flow<List<SocialPost>> {
        return contentSource.observeMyDraftPosts()
    }

    fun observeSavedPosts(): Flow<List<SocialPost>> {
        return contentSource.observeSavedPosts()
    }

    fun observePost(postId: String): Flow<SocialPost?> {
        return contentSource.observePost(postId)
    }

    suspend fun getPostById(postId: String): SocialPost? {
        return contentSource.getPostById(postId)
    }
}
