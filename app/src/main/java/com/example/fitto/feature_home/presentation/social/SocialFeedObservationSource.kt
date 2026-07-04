package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal interface SocialFeedObservationSource {
    fun observeFeed(): Flow<List<SocialPost>>

    suspend fun refreshFeedFromServer()
}
