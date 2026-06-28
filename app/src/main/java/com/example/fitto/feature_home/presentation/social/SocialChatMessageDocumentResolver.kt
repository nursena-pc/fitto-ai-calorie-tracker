package com.example.fitto.feature_home.presentation.social

internal object SocialChatMessageDocumentResolver {

    fun resolve(
        id: String,
        createdAt: Long,
        currentUserId: String,
        currentUserClearedAt: Long,
        deletedFor: List<String>,
        senderId: String,
        senderName: String?,
        senderPhotoUrl: String?,
        fallbackPhotoUrl: String?,
        participantNames: Map<String, String>,
        participantPhotoUrls: Map<String, String?>,
        readBy: Map<String, Long>,
        totalRecipientCount: Int,
        isDeleted: Boolean,
        messageType: String?,
        text: String?,
        fileName: String?,
        fileUrl: String?,
        fileMimeType: String?,
        fileSizeBytes: Long,
        audioDurationMillis: Long,
        deliveredAt: Long?,
        deletedAt: Long,
        deletedBy: String?
    ): ChatMessageUiModel? {
        if (createdAt <= currentUserClearedAt) return null
        if (deletedFor.contains(currentUserId)) return null

        val resolvedType = messageType.orEmpty().ifBlank { "text" }
        val readByOthersCount = readBy.keys
            .filter { uid -> uid != senderId }
            .distinct()
            .size

        val messageText = if (isDeleted) {
            "Bu mesaj silindi"
        } else {
            when {
                resolvedType == "file" && fileMimeType?.startsWith("image/") == true -> ""
                resolvedType == "file" -> fileName ?: "Dosya"
                resolvedType == "audio" -> "Ses mesajı"
                else -> text.orEmpty()
            }
        }

        return ChatMessageUiModel(
            id = id,
            text = messageText,
            timeText = SocialTextResolver.formatClockTime(createdAt),
            senderId = senderId,
            senderName = senderName.orEmpty().ifBlank { participantNames[senderId] ?: "Fitto" },
            senderPhotoUrl = senderPhotoUrl ?: fallbackPhotoUrl ?: participantPhotoUrls[senderId],
            isFromMe = senderId == currentUserId,
            createdAt = createdAt,
            deliveredAt = deliveredAt ?: createdAt,
            readBy = readBy,
            type = resolvedType,
            fileUrl = fileUrl,
            fileName = fileName,
            fileMimeType = fileMimeType,
            fileSizeBytes = fileSizeBytes,
            participantNames = participantNames,
            readReceiptText = if (senderId == currentUserId) {
                SocialReadReceiptResolver.createReadReceiptText(readByOthersCount, totalRecipientCount)
            } else {
                null
            },
            readByCount = readByOthersCount,
            totalRecipientCount = totalRecipientCount,
            isDeleted = isDeleted,
            audioDurationMillis = audioDurationMillis,
            deletedAt = deletedAt,
            deletedBy = deletedBy
        )
    }
}
