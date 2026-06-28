package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue

internal object SocialMessageThreadUpdateResolver {

    fun visibleRecipientIds(
        participantIds: List<String>,
        senderId: String,
        blockedRecipientIds: List<String>
    ): List<String> {
        return participantIds.filter { participantId ->
            participantId != senderId && !blockedRecipientIds.contains(participantId)
        }
    }

    fun createLastMessageUpdates(
        lastMessageText: String,
        lastMessageSenderId: String,
        lastMessageCreatedAt: Long,
        lastMessageType: String,
        participantIds: List<String>,
        senderId: String,
        blockedRecipientIds: List<String>,
        updatedAt: Long
    ): MutableMap<String, Any> {
        val updates = mutableMapOf<String, Any>(
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageCreatedAt" to lastMessageCreatedAt,
            "lastMessageType" to lastMessageType,
            "lastMessageHiddenFor" to blockedRecipientIds,
            "updatedAt" to updatedAt,
            "unreadCounts.$senderId" to 0
        )

        visibleRecipientIds(
            participantIds = participantIds,
            senderId = senderId,
            blockedRecipientIds = blockedRecipientIds
        ).forEach { participantId ->
            updates["unreadCounts.$participantId"] = FieldValue.increment(1)
        }

        blockedRecipientIds.forEach { participantId ->
            updates["unreadCounts.$participantId"] = 0
        }

        return updates
    }
}
