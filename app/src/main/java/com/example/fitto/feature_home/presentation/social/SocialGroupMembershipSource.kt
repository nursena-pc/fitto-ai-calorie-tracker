package com.example.fitto.feature_home.presentation.social

internal interface SocialGroupMembershipSource {
    suspend fun leaveGroup(threadId: String)

    suspend fun removeMemberFromGroup(
        threadId: String,
        memberId: String
    )

    suspend fun addMembersToGroup(
        threadId: String,
        selectedUsers: List<SearchUserUiModel>
    )
}
