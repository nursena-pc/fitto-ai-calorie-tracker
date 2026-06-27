package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.DocumentSnapshot

internal data class BlockedThreadPreview(
    val text: String,
    val createdAt: Long
)

internal object SocialMessageHiddenRangeResolver {

    fun readBlockedThreadPreviews(value: Any?): Map<String, BlockedThreadPreview> {
        val rawMap = value as? Map<*, *> ?: return emptyMap()

        return rawMap.mapNotNull { entry ->
            val threadId = entry.key as? String ?: return@mapNotNull null
            val previewMap = entry.value as? Map<*, *> ?: return@mapNotNull null

            val text = previewMap["text"] as? String ?: "Henüz mesaj yok."
            val createdAt = (previewMap["createdAt"] as? Number)?.toLong() ?: 0L

            threadId to BlockedThreadPreview(
                text = text,
                createdAt = createdAt
            )
        }.toMap()
    }

    fun readMessageHiddenRange(value: Any?): MessageHiddenRange? {
        val map = value as? Map<*, *> ?: return null

        val userId = map["userId"] as? String ?: return null
        val startAt = (map["startAt"] as? Number)?.toLong() ?: return null
        val endAt = (map["endAt"] as? Number)?.toLong()

        return MessageHiddenRange(
            userId = userId,
            startAt = startAt,
            endAt = endAt
        )
    }

    fun readCompletedHiddenRanges(
        snapshot: DocumentSnapshot,
        threadId: String
    ): List<MessageHiddenRange> {
        return (snapshot.get("blockedMessageRanges.$threadId") as? List<*>)
            ?.mapNotNull { item ->
                readMessageHiddenRange(item)
            }
            .orEmpty()
    }

    fun readActiveHiddenRange(
        snapshot: DocumentSnapshot,
        threadId: String
    ): MessageHiddenRange? {
        return readMessageHiddenRange(
            snapshot.get("activeBlockedMessageRanges.$threadId")
        )
    }

    fun isMessageInHiddenRanges(
        senderId: String,
        createdAt: Long,
        hiddenRanges: List<MessageHiddenRange>
    ): Boolean {
        return hiddenRanges.any { range ->
            range.userId == senderId &&
                    createdAt >= range.startAt &&
                    (range.endAt == null || createdAt <= range.endAt)
        }
    }
}
