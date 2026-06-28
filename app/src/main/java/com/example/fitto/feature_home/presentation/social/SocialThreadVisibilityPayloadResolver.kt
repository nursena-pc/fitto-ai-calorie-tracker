package com.example.fitto.feature_home.presentation.social

internal object SocialThreadVisibilityPayloadResolver {

    fun isParticipant(
        participantIds: List<String>,
        currentUserId: String
    ): Boolean {
        return currentUserId.isNotBlank() && participantIds.contains(currentUserId)
    }

    fun createClearPayload(
        currentUserId: String,
        clearedAt: Long
    ): Map<String, Any> {
        return mapOf(
            "clearedAt.$currentUserId" to clearedAt,
            "unreadCounts.$currentUserId" to 0,
            "updatedAt" to clearedAt
        )
    }
}
