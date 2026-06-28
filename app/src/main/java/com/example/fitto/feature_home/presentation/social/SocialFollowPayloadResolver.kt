package com.example.fitto.feature_home.presentation.social

internal object SocialFollowPayloadResolver {

    fun shouldSkipFollowAction(
        currentUserId: String?,
        targetUserId: String
    ): Boolean {
        return currentUserId.isNullOrBlank() ||
            targetUserId.isBlank() ||
            currentUserId == targetUserId
    }

    fun followDocumentId(
        followerId: String,
        followingId: String
    ): String = "${followerId}_${followingId}"

    fun isDeletedOrMissing(
        exists: Boolean,
        isDeleted: Boolean?
    ): Boolean = !exists || isDeleted == true

    fun createFollowPayload(
        followerId: String,
        followingId: String,
        createdAt: Long
    ): Map<String, Any> {
        return mapOf(
            "followerId" to followerId,
            "followingId" to followingId,
            "createdAt" to createdAt
        )
    }

    fun deletedUserCountResetPayload(): Map<String, Any> {
        return mapOf(
            "followersCount" to 0,
            "followingCount" to 0
        )
    }

    fun decrementCountPayload(
        field: String,
        currentCount: Long?,
        updatedAt: Long
    ): Map<String, Any> {
        return mapOf(
            field to ((currentCount ?: 0L) - 1L).coerceAtLeast(0L),
            "updatedAt" to updatedAt
        )
    }
}
