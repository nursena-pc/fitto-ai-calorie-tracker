package com.example.fitto.feature_home.presentation.social

data class GroupSettingsUiModel(
    val joinApprovalRequired: Boolean = false,
    val membersCanSendMessages: Boolean = true,
    val membersCanAddMembers: Boolean = true,
    val membersCanEditGroupInfo: Boolean = false
)