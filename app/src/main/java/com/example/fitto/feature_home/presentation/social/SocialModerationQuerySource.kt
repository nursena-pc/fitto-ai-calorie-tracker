package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal interface SocialModerationQuerySource {
    suspend fun getBlockedUsers(): List<SocialUser>

    suspend fun isUserBlocked(targetUserId: String): Boolean

    suspend fun getDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): DirectThreadBlockState

    fun observeDirectThreadBlockState(
        threadId: String,
        targetUserId: String
    ): Flow<DirectThreadBlockState>
}
