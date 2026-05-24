package com.example.fitto.feature_home.presentation.social

data class GroupMessageSearchResultUiModel(
    val messageId: String,
    val threadId: String,
    val senderName: String,
    val messageText: String,
    val createdAt: Long
)