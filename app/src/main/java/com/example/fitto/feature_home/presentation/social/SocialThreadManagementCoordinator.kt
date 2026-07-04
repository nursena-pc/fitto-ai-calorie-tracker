package com.example.fitto.feature_home.presentation.social

import android.net.Uri

internal class SocialThreadManagementCoordinator(
    private val querySource: SocialThreadQuerySource,
    private val commandSource: SocialThreadCommandSource,
    private val membershipSource: SocialGroupMembershipSource
) {
    suspend fun getGroupSettings(threadId: String): GroupSettingsUiModel {
        return querySource.getGroupSettings(threadId)
    }

    suspend fun getGroupMembers(threadId: String): List<GroupMemberUiModel> {
        return querySource.getGroupMembers(threadId)
    }

    suspend fun getThreadSharedContent(threadId: String): List<SharedContentUiModel> {
        return querySource.getThreadSharedContent(threadId)
    }

    suspend fun searchUsersForGroupInvite(
        threadId: String,
        query: String
    ): List<SearchUserUiModel> {
        return querySource.searchUsersForGroupInvite(threadId, query)
    }

    suspend fun getMessageThread(threadId: String): MessageThreadUiModel? {
        return querySource.getMessageThread(threadId)
    }

    suspend fun getMessageThreadRawData(threadId: String): Map<String, Any?> {
        return querySource.getMessageThreadRawData(threadId)
    }

    suspend fun createOrGetDirectThread(targetUserId: String): MessageThreadUiModel? {
        return commandSource.createOrGetDirectThread(targetUserId)
    }

    suspend fun createGroupThread(
        title: String,
        description: String,
        selectedUsers: List<SearchUserUiModel>
    ): MessageThreadUiModel? {
        return commandSource.createGroupThread(title, description, selectedUsers)
    }

    suspend fun updateGroupSettings(
        threadId: String,
        settings: GroupSettingsUiModel
    ) {
        commandSource.updateGroupSettings(threadId, settings)
    }

    suspend fun updateGroupPhoto(
        threadId: String,
        imageUri: Uri
    ): String {
        return commandSource.updateGroupPhoto(threadId, imageUri)
    }

    suspend fun updateGroupTitle(threadId: String, newTitle: String) {
        commandSource.updateGroupTitle(threadId, newTitle)
    }

    suspend fun updateGroupDescription(threadId: String, newDescription: String) {
        commandSource.updateGroupDescription(threadId, newDescription)
    }

    suspend fun updateGroupName(threadId: String, newName: String) {
        commandSource.updateGroupName(threadId, newName)
    }

    suspend fun clearThreadForCurrentUser(threadId: String) {
        commandSource.clearThreadForCurrentUser(threadId)
    }

    suspend fun deleteThreadForCurrentUser(threadId: String) {
        commandSource.deleteThreadForCurrentUser(threadId)
    }

    suspend fun leaveGroup(threadId: String) {
        membershipSource.leaveGroup(threadId)
    }

    suspend fun removeMemberFromGroup(threadId: String, memberId: String) {
        membershipSource.removeMemberFromGroup(threadId, memberId)
    }

    suspend fun addMembersToGroup(
        threadId: String,
        selectedUsers: List<SearchUserUiModel>
    ) {
        membershipSource.addMembersToGroup(threadId, selectedUsers)
    }
}
