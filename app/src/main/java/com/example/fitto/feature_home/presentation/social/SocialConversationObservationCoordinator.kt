package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal class SocialConversationObservationCoordinator(
    private val realtimeSource: SocialRealtimeObservationSource,
    private val threadSource: SocialThreadObservationSource
) {
    fun observeMessageThreads(): Flow<List<MessageThreadUiModel>> {
        return threadSource.observeMessageThreads()
    }

    fun observeMessages(threadId: String): Flow<List<ChatMessageUiModel>> {
        return threadSource.observeMessages(threadId)
    }

    fun observeThreadMessagesForSearch(
        threadId: String
    ): Flow<List<GroupMessageSearchResultUiModel>> {
        return threadSource.observeThreadMessagesForSearch(threadId)
    }

    fun observeUserPresence(userId: String): Flow<PresenceStatusUiModel> {
        return realtimeSource.observeUserPresence(userId)
    }

    fun observeThreadSharedContentSummary(
        threadId: String
    ): Flow<ThreadSharedContentSummary> {
        return threadSource.observeThreadSharedContentSummary(threadId)
    }

    fun observeTypingUsers(threadId: String): Flow<List<TypingUserUiModel>> {
        return realtimeSource.observeTypingUsers(threadId)
    }

    fun setTyping(threadId: String, isTyping: Boolean) {
        realtimeSource.setTyping(threadId, isTyping)
    }

    fun observeUnreadMessageCount(): Flow<Int> {
        return threadSource.observeUnreadMessageCount()
    }
}
