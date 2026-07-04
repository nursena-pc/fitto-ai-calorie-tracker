package com.example.fitto.feature_home.presentation.social

internal interface SocialThreadQuerySource {
    suspend fun getGroupSettings(threadId: String): GroupSettingsUiModel

    suspend fun getGroupMembers(threadId: String): List<GroupMemberUiModel>

    suspend fun getThreadSharedContent(threadId: String): List<SharedContentUiModel>

    suspend fun searchUsersForGroupInvite(
        threadId: String,
        query: String
    ): List<SearchUserUiModel>

    suspend fun getMessageThread(threadId: String): MessageThreadUiModel?

    suspend fun getMessageThreadRawData(threadId: String): Map<String, Any?>
}
