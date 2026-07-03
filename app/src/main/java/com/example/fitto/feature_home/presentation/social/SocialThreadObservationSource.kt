package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow

internal interface SocialThreadObservationSource {
    fun observeThreadMessagesForSearch(
        threadId: String
    ): Flow<List<GroupMessageSearchResultUiModel>>

    fun observeThreadSharedContentSummary(
        threadId: String
    ): Flow<ThreadSharedContentSummary>

    fun observeUnreadMessageCount(): Flow<Int>
}
