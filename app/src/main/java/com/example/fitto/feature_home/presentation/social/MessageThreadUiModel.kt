package com.example.fitto.feature_home.presentation.social

data class MessageThreadUiModel(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timeText: String,
    val isGroup: Boolean,
    val highlightText: String? = null,
    val initials: String,
    val unreadCount: Int = 0,
    val participantIds: List<String> = emptyList(),
    val otherParticipantId: String? = null,
    val photoUrl: String? = null,
    val memberCount: Int = 0,
    val lastMessageCreatedAt: Long = 0L
)
