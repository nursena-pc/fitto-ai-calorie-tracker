package com.example.fitto.feature_home.presentation.social

import android.net.Uri

internal interface SocialThreadCommandSource {
    suspend fun createOrGetDirectThread(targetUserId: String): MessageThreadUiModel?

    suspend fun createGroupThread(
        title: String,
        description: String,
        selectedUsers: List<SearchUserUiModel>
    ): MessageThreadUiModel?

    suspend fun updateGroupSettings(
        threadId: String,
        settings: GroupSettingsUiModel
    )

    suspend fun updateGroupPhoto(
        threadId: String,
        imageUri: Uri
    ): String

    suspend fun updateGroupTitle(threadId: String, newTitle: String)

    suspend fun updateGroupDescription(threadId: String, newDescription: String)

    suspend fun updateGroupName(threadId: String, newName: String)

    suspend fun clearThreadForCurrentUser(threadId: String)

    suspend fun deleteThreadForCurrentUser(threadId: String)
}
