package com.example.fitto.feature_home.presentation.social

internal object SocialGroupSettingsResolver {

    private const val JOIN_APPROVAL_REQUIRED = "joinApprovalRequired"
    private const val MEMBERS_CAN_SEND_MESSAGES = "membersCanSendMessages"
    private const val MEMBERS_CAN_ADD_MEMBERS = "membersCanAddMembers"
    private const val MEMBERS_CAN_EDIT_GROUP_INFO = "membersCanEditGroupInfo"

    fun fromRawMap(settings: Map<*, *>?): GroupSettingsUiModel {
        return GroupSettingsUiModel(
            joinApprovalRequired = settings?.get(JOIN_APPROVAL_REQUIRED) as? Boolean ?: false,
            membersCanSendMessages = settings?.get(MEMBERS_CAN_SEND_MESSAGES) as? Boolean ?: true,
            membersCanAddMembers = settings?.get(MEMBERS_CAN_ADD_MEMBERS) as? Boolean ?: true,
            membersCanEditGroupInfo = settings?.get(MEMBERS_CAN_EDIT_GROUP_INFO) as? Boolean ?: false
        )
    }

    fun toRawMap(settings: GroupSettingsUiModel): Map<String, Boolean> {
        return mapOf(
            JOIN_APPROVAL_REQUIRED to settings.joinApprovalRequired,
            MEMBERS_CAN_SEND_MESSAGES to settings.membersCanSendMessages,
            MEMBERS_CAN_ADD_MEMBERS to settings.membersCanAddMembers,
            MEMBERS_CAN_EDIT_GROUP_INFO to settings.membersCanEditGroupInfo
        )
    }
}
