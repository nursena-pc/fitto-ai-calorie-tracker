package com.example.fitto.feature_home.presentation.social

internal object SocialGroupMessageSearchResolver {

    private const val FALLBACK_SENDER_NAME = "Kullanıcı"

    fun shouldSkipSearch(threadId: String): Boolean {
        return threadId.isBlank()
    }

    fun resolveResult(
        messageId: String,
        threadId: String,
        text: String?,
        senderName: String?,
        senderDisplayName: String?,
        createdAt: Long?
    ): GroupMessageSearchResultUiModel? {
        val trimmedText = text.orEmpty().trim()
        if (trimmedText.isBlank()) return null

        return GroupMessageSearchResultUiModel(
            messageId = messageId,
            threadId = threadId,
            senderName = senderName ?: senderDisplayName ?: FALLBACK_SENDER_NAME,
            messageText = trimmedText,
            createdAt = createdAt ?: 0L
        )
    }
}
