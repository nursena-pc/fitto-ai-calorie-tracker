package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.FieldValue

internal object SocialModerationPayloadResolver {

    private const val DEFAULT_BLOCKED_THREAD_PREVIEW = "Henüz mesaj yok."

    fun shouldSkipTargetAction(
        currentUserId: String,
        targetUserId: String
    ): Boolean {
        return targetUserId.isBlank() || targetUserId == currentUserId
    }

    fun createPostReportPayload(
        postId: String,
        reporterId: String,
        reason: String,
        createdAt: Long
    ): Map<String, Any> {
        return mapOf(
            "postId" to postId,
            "reporterId" to reporterId,
            "reason" to reason,
            "createdAt" to createdAt
        )
    }

    fun createUserReportPayload(
        targetUserId: String,
        reporterId: String,
        threadId: String,
        reason: String,
        createdAt: Long
    ): Map<String, Any> {
        return mapOf(
            "type" to "user",
            "reportedUserId" to targetUserId,
            "reporterId" to reporterId,
            "threadId" to threadId,
            "reason" to reason,
            "createdAt" to createdAt
        )
    }

    fun createBlockUserPayload(
        targetUserId: String,
        threadId: String?,
        previewText: String?,
        previewCreatedAt: Long?,
        updatedAt: Long
    ): Map<String, Any> {
        val updates = mutableMapOf<String, Any>(
            "blockedUserIds" to FieldValue.arrayUnion(targetUserId),
            "updatedAt" to updatedAt
        )

        if (!threadId.isNullOrBlank()) {
            updates["activeBlockedMessageRanges.$threadId"] = mapOf(
                "userId" to targetUserId,
                "startAt" to updatedAt
            )
            updates["blockedThreadPreviews.$threadId"] = mapOf(
                "text" to previewText.orEmpty().ifBlank { DEFAULT_BLOCKED_THREAD_PREVIEW },
                "createdAt" to (previewCreatedAt ?: updatedAt)
            )
        }

        return updates
    }

    fun createUnblockBasePayload(
        targetUserId: String,
        updatedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "blockedUserIds" to FieldValue.arrayRemove(targetUserId),
            "updatedAt" to updatedAt
        )
    }

    fun createCloseBlockedThreadPayload(
        threadId: String,
        activeRange: MessageHiddenRange?,
        targetUserId: String,
        updatedAt: Long
    ): Map<String, Any> {
        val updates = mutableMapOf<String, Any>()

        if (activeRange != null && activeRange.userId == targetUserId) {
            updates["blockedMessageRanges.$threadId"] = FieldValue.arrayUnion(
                mapOf(
                    "userId" to activeRange.userId,
                    "startAt" to activeRange.startAt,
                    "endAt" to updatedAt
                )
            )
        }

        updates["activeBlockedMessageRanges.$threadId"] = FieldValue.delete()
        updates["blockedThreadPreviews.$threadId"] = FieldValue.delete()

        return updates
    }
}
