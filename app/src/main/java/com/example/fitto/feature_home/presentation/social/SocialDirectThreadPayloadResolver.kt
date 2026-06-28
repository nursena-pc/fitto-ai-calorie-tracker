package com.example.fitto.feature_home.presentation.social

internal object SocialDirectThreadPayloadResolver {

    fun canCreateDirectThread(
        currentUserId: String,
        targetUserId: String
    ): Boolean {
        return currentUserId.isNotBlank() &&
            targetUserId.isNotBlank() &&
            currentUserId != targetUserId
    }

    fun currentUserName(
        profileUserName: String?,
        email: String?
    ): String {
        return profileUserName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")
            ?: DEFAULT_USER_NAME
    }

    fun targetUserName(userName: String?): String {
        return userName?.ifBlank { null } ?: DEFAULT_USER_NAME
    }

    fun createPayload(
        threadId: String,
        currentUserId: String,
        targetUserId: String,
        currentUserName: String,
        currentUserPhotoUrl: String?,
        targetUserName: String,
        targetUserPhotoUrl: String?,
        now: Long,
        participantMapFactory: (uid: String, userName: String, photoUrl: String?) -> Map<String, Any?>
    ): Map<String, Any?> {
        val participantIds = listOf(currentUserId, targetUserId).sorted()

        return mapOf(
            "id" to threadId,
            "type" to THREAD_TYPE_DIRECT,
            "title" to null,
            "participantIds" to participantIds,
            "memberCount" to DIRECT_MEMBER_COUNT,
            "participants" to mapOf(
                currentUserId to participantMapFactory(
                    currentUserId,
                    currentUserName,
                    currentUserPhotoUrl
                ),
                targetUserId to participantMapFactory(
                    targetUserId,
                    targetUserName,
                    targetUserPhotoUrl
                )
            ),
            "unreadCounts" to mapOf(
                currentUserId to 0,
                targetUserId to 0
            ),
            "lastMessageText" to "",
            "lastMessageSenderId" to null,
            "lastMessageCreatedAt" to null,
            "createdAt" to now,
            "deliveredAt" to now,
            "updatedAt" to now
        )
    }

    private const val THREAD_TYPE_DIRECT = "direct"
    private const val DIRECT_MEMBER_COUNT = 2
    private const val DEFAULT_USER_NAME = "fitto_user"
}
