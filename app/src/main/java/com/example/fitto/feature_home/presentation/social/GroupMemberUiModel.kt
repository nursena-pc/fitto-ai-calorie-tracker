package com.example.fitto.feature_home.presentation.social

data class GroupMemberUiModel(
    val uid: String,
    val userName: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val isCreator: Boolean
)