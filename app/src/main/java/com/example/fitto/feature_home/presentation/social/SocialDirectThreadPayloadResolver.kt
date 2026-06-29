package com.example.fitto.feature_home.presentation.social

internal data class SocialDirectThreadParticipant(
    val uid: String,
    val userName: String,
    val photoUrl: String?
)

internal object SocialDirectThreadPayloadResolver {

    fun shouldSkipTarget(
        currentUserId: String,
        targetUserId: String
    ): Boolean {
        return targetUserId.isBlank() || targetUserId == currentUserId
    }

    fun currentParticipant(
        uid: String,
        profileUserName: String?,
        profilePhotoUrl: String?,
        email: String?,
        authPhotoUrl: String?
    ): SocialDirectThreadParticipant {
        return SocialDirectThreadParticipant(
            uid = uid,
            userName = profileUserName?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore("@")
                ?: DEFAULT_USER_NAME,
            photoUrl = profilePhotoUrl ?: authPhotoUrl
        )
    }

    fun targetParticipant(
        uid: String,
        userName: String,
        photoUrl: String?
    ): SocialDirectThreadParticipant {
        return SocialDirectThreadParticipant(
            uid = uid,
            userName = userName.ifBlank { DEFAULT_USER_NAME },
            photoUrl = photoUrl
        )
    }

    fun createPayload(
        threadId: String,
        threadType: String,
        currentParticipant: SocialDirectThreadParticipant,
        targetParticipant: SocialDirectThreadParticipant,
        now: Long
    ): Map<String, Any?> {
        val participantIds = listOf(currentParticipant.uid, targetParticipant.uid).sorted()

        return mapOf(
            "id" to threadId,
            "type" to threadType,
            "title" to null,
            "participantIds" to participantIds,
            "memberCount" to participantIds.size,
            "participants" to mapOf(
                currentParticipant.uid to SocialThreadParticipantResolver.participantMap(
                    uid = currentParticipant.uid,
                    userName = currentParticipant.userName,
                    photoUrl = currentParticipant.photoUrl
                ),
                targetParticipant.uid to SocialThreadParticipantResolver.participantMap(
                    uid = targetParticipant.uid,
                    userName = targetParticipant.userName,
                    photoUrl = targetParticipant.photoUrl
                )
            ),
            "unreadCounts" to participantIds.associateWith { 0 },
            "lastMessageText" to "",
            "lastMessageSenderId" to null,
            "lastMessageCreatedAt" to null,
            "createdAt" to now,
            "deliveredAt" to now,
            "updatedAt" to now
        )
    }

    private const val DEFAULT_USER_NAME = "fitto_user"
}
