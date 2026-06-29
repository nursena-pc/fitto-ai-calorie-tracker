package com.example.fitto.feature_home.presentation.social

internal object SocialRealtimePresenceResolver {

    fun shouldSkipUserPresence(userId: String): Boolean {
        return userId.isBlank()
    }

    fun offlineStatus(userId: String): PresenceStatusUiModel {
        return PresenceStatusUiModel(
            userId = userId,
            isOnline = false,
            lastChanged = 0L
        )
    }

    fun resolvePresenceStatus(
        userId: String,
        state: String?,
        lastChanged: Long?,
        onlineState: String
    ): PresenceStatusUiModel {
        return PresenceStatusUiModel(
            userId = userId,
            isOnline = state == onlineState,
            lastChanged = lastChanged ?: 0L
        )
    }

    fun shouldSkipTypingUsers(
        threadId: String,
        currentUserId: String
    ): Boolean {
        return threadId.isBlank() || currentUserId.isBlank()
    }

    fun shouldSkipTypingUpdate(
        threadId: String,
        userId: String?
    ): Boolean {
        return threadId.isBlank() || userId.isNullOrBlank()
    }

    fun shouldRemoveTyping(isTyping: Boolean): Boolean {
        return !isTyping
    }
}
