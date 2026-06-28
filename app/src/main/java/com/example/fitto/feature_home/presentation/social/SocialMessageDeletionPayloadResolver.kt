package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue

internal object SocialMessageDeletionPayloadResolver {

    fun createDeleteForMePayload(
        currentUserId: String,
        updatedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "deletedFor" to FieldValue.arrayUnion(currentUserId),
            "updatedAt" to updatedAt
        )
    }

    fun createDeleteForEveryonePayload(
        currentUserId: String,
        deletedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "text" to "",
            "isDeleted" to true,
            "deletedBy" to currentUserId,
            "deletedAt" to deletedAt,
            "updatedAt" to deletedAt
        )
    }

    fun createLastMessageDeletedPayload(updatedAt: Long): Map<String, Any> {
        return mapOf(
            "lastMessageText" to "Bu mesaj silindi",
            "updatedAt" to updatedAt
        )
    }
}
