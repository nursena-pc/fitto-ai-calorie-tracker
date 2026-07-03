package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal interface SocialRealtimeObservationSource {
    fun observeUserPresence(userId: String): Flow<PresenceStatusUiModel>

    fun observeTypingUsers(threadId: String): Flow<List<TypingUserUiModel>>

    fun setTyping(threadId: String, isTyping: Boolean)
}
