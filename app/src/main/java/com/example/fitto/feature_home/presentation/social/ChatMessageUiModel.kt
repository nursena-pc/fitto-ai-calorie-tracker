package com.example.fitto.feature_home.presentation.social

data class ChatMessageUiModel(
    val id: String,
    val text: String,
    val timeText: String,
    val senderId: String,
    val senderName: String,
    val senderPhotoUrl: String? = null,
    val isFromMe: Boolean,
    val createdAt: Long = 0L,
    val readBy: Map<String, Long> = emptyMap(),
    val participantNames: Map<String, String> = emptyMap(),
    val deliveredAt: Long = 0L,
    val readReceiptText: String? = null,
    val readByCount: Int = 0,
    val totalRecipientCount: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    val deletedBy: String? = null,
    val type: String = "text",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val audioDurationMillis: Long = 0L
)
