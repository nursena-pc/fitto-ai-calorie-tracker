package com.example.fitto.feature_home.presentation.social

internal object SocialMessageListResolver {

    private const val DEFAULT_PARTICIPANT_NAME = "fitto_user"

    fun shouldSkipMessageList(
        currentUserId: String?,
        threadId: String
    ): Boolean {
        return currentUserId.isNullOrBlank() || threadId.isBlank()
    }

    fun participantNames(
        participantIds: List<String>,
        participants: Map<*, *>?
    ): Map<String, String> {
        return participantIds.associateWith { uid ->
            participantMap(participants, uid)
                ?.get("userName")
                .asStringOrNull()
                .orEmpty()
                .ifBlank { DEFAULT_PARTICIPANT_NAME }
        }
    }

    fun participantPhotoUrls(
        participantIds: List<String>,
        participants: Map<*, *>?,
        latestParticipants: Map<String, SocialUser>
    ): Map<String, String?> {
        return participantIds.associateWith { uid ->
            latestParticipants[uid]?.photoUrl
                ?: participantMap(participants, uid)
                    ?.get("photoUrl")
                    .asStringOrNull()
        }
    }

    fun totalRecipientCount(participantIds: List<String>): Int {
        return (participantIds.size - 1).coerceAtLeast(1)
    }

    fun currentUserClearedAt(
        clearedAt: Any?,
        currentUserId: String
    ): Long {
        return ((clearedAt as? Map<*, *>)?.get(currentUserId) as? Number)?.toLong() ?: 0L
    }

    private fun participantMap(
        participants: Map<*, *>?,
        uid: String
    ): Map<*, *>? {
        return participants?.get(uid) as? Map<*, *>
    }

    private fun Any?.asStringOrNull(): String? {
        return this as? String
    }
}
