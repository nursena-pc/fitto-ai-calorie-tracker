package com.example.fitto.feature_home.presentation.social

internal data class SocialMessageRecipientBlockRecord(
    val userId: String,
    val blockedUserIds: List<String>
)

internal object SocialMessageDeliveryResolver {

    private const val CHAT_ATTACHMENTS_ROOT = "chat_attachments"
    private const val AUDIO_FILE_NAME = "voice_message.m4a"
    private const val AUDIO_MIME_TYPE = "audio/mp4"
    private const val AUDIO_LAST_MESSAGE_TEXT = "🎤 Ses mesajı"
    private const val FIRESTORE_WHERE_IN_LIMIT = 10

    fun shouldSkipTextMessage(
        threadId: String,
        text: String
    ): Boolean {
        return threadId.isBlank() || text.trim().isBlank()
    }

    fun shouldSkipAttachmentMessage(threadId: String): Boolean {
        return threadId.isBlank()
    }

    fun shouldSkipAudioMessage(
        threadId: String,
        fileExists: Boolean,
        fileSizeBytes: Long
    ): Boolean {
        return threadId.isBlank() || !fileExists || fileSizeBytes <= 0L
    }

    fun recipientIds(
        participantIds: List<String>,
        senderId: String
    ): List<String> {
        return participantIds
            .map { participantId -> participantId.trim() }
            .filter { participantId -> participantId.isNotBlank() && participantId != senderId }
            .distinct()
    }

    fun recipientLookupChunks(
        participantIds: List<String>,
        senderId: String
    ): List<List<String>> {
        return recipientIds(
            participantIds = participantIds,
            senderId = senderId
        ).chunked(FIRESTORE_WHERE_IN_LIMIT)
    }

    fun blockedRecipientIds(
        records: List<SocialMessageRecipientBlockRecord>,
        senderId: String
    ): List<String> {
        return records
            .filter { record -> record.blockedUserIds.contains(senderId) }
            .map { record -> record.userId }
            .distinct()
    }

    fun attachmentStoragePath(
        threadId: String,
        messageId: String,
        fileId: String,
        safeFileName: String
    ): String {
        return listOf(
            CHAT_ATTACHMENTS_ROOT,
            threadId,
            messageId,
            "${fileId}_${safeFileName}"
        ).joinToString("/")
    }

    fun audioStoragePath(
        threadId: String,
        messageId: String,
        fileId: String
    ): String {
        return attachmentStoragePath(
            threadId = threadId,
            messageId = messageId,
            fileId = fileId,
            safeFileName = AUDIO_FILE_NAME
        )
    }

    fun audioContentType(): String {
        return AUDIO_MIME_TYPE
    }

    fun audioLastMessageText(): String {
        return AUDIO_LAST_MESSAGE_TEXT
    }
}
