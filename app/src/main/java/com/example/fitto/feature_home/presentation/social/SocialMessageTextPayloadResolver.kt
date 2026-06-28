package com.example.fitto.feature_home.presentation.social

internal object SocialMessageTextPayloadResolver {

    fun createTextMessagePayload(
        messageId: String,
        threadId: String,
        senderId: String,
        sender: SocialMessageSender,
        text: String,
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
            "text" to text,
            "type" to "text",
            "readBy" to mapOf(senderId to readAt),
            "deletedFor" to deletedFor,
            "createdAt" to readAt,
            "deliveredAt" to readAt
        )
    }
}
