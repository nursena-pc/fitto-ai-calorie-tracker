package com.example.fitto.feature_home.presentation.social

internal object SocialMessageAttachmentPayloadResolver {

    fun safeStorageFileName(
        fileName: String,
        fallbackFileName: String
    ): String {
        return fileName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { fallbackFileName }
    }

    fun fileLastMessageText(
        fileName: String,
        mimeType: String?
    ): String {
        return if (mimeType?.startsWith("image/") == true) {
            "🖼️ Fotoğraf"
        } else {
            "📎 $fileName"
        }
    }

    fun createFileMessagePayload(
        messageId: String,
        threadId: String,
        senderId: String,
        sender: SocialMessageSender,
        fileUrl: String,
        fileName: String,
        fileMimeType: String?,
        fileSizeBytes: Long,
        deletedFor: List<String>,
        readAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "id" to messageId,
            "threadId" to threadId,
            "senderId" to senderId,
            "senderName" to sender.name,
            "senderPhotoUrl" to sender.photoUrl,
            "photoUrl" to sender.photoUrl,
            "text" to fileName,
            "type" to "file",
            "fileUrl" to fileUrl,
            "deletedFor" to deletedFor,
            "fileName" to fileName,
            "fileMimeType" to fileMimeType,
            "fileSizeBytes" to fileSizeBytes,
            "readBy" to mapOf(senderId to readAt),
            "createdAt" to readAt,
            "deliveredAt" to readAt
        )
    }

    fun createAudioMessagePayload(
        messageId: String,
        threadId: String,
        senderId: String,
        sender: SocialMessageSender,
        fileUrl: String,
        fileSizeBytes: Long,
        audioDurationMillis: Long,
        deletedFor: List<String>,
        readAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "id" to messageId,
            "threadId" to threadId,
            "senderId" to senderId,
            "senderName" to sender.name,
            "senderPhotoUrl" to sender.photoUrl,
            "photoUrl" to sender.photoUrl,
            "text" to "Ses mesajı",
            "type" to "audio",
            "fileUrl" to fileUrl,
            "deletedFor" to deletedFor,
            "fileName" to "voice_message.m4a",
            "fileMimeType" to "audio/mp4",
            "fileSizeBytes" to fileSizeBytes,
            "audioDurationMillis" to audioDurationMillis,
            "readBy" to mapOf(senderId to readAt),
            "createdAt" to readAt,
            "deliveredAt" to readAt
        )
    }
}
