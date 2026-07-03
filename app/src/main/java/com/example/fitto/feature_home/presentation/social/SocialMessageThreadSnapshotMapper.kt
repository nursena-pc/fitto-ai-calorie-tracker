package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.DocumentSnapshot

internal object SocialMessageThreadSnapshotMapper {
    fun map(
        document: DocumentSnapshot,
        currentUserId: String,
        includeClearedThread: Boolean = false
    ): MessageThreadUiModel? {
        if (!document.exists()) return null

        return SocialMessageThreadDocumentResolver.resolve(
            id = document.id,
            type = document.getString("type"),
            currentUserId = currentUserId,
            participantIds = document.getStringListCompat("participantIds"),
            participants = document.get("participants") as? Map<*, *>,
            groupTitle = document.getString("title"),
            groupPhotoUrl = document.getString("photoUrl"),
            lastMessageText = document.getString("lastMessageText"),
            lastMessageCreatedAt = document.getLong("lastMessageCreatedAt"),
            updatedAt = document.getLong("updatedAt"),
            clearedAt = document.get("clearedAt") as? Map<*, *>,
            unreadCounts = document.get("unreadCounts") as? Map<*, *>,
            memberCount = document.getLong("memberCount")?.toInt(),
            includeClearedThread = includeClearedThread
        )
    }

    private fun DocumentSnapshot.getStringListCompat(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { value -> value as? String }
            .orEmpty()
    }
}
