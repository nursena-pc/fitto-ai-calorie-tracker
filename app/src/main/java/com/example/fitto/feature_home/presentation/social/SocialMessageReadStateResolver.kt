package com.example.fitto.feature_home.presentation.social

internal object SocialMessageReadStateResolver {

    fun canMarkThreadRead(
        currentUserId: String?,
        threadId: String
    ): Boolean {
        return !currentUserId.isNullOrBlank() && threadId.isNotBlank()
    }

    fun directPeerId(
        threadType: String?,
        participantIds: List<String>,
        currentUserId: String
    ): String? {
        if (threadType != SocialRepository.THREAD_TYPE_DIRECT) return null
        return participantIds.firstOrNull { participantId ->
            participantId.isNotBlank() && participantId != currentUserId
        }
    }

    fun blockedUserIds(rawBlockedUserIds: Any?): List<String> {
        return (rawBlockedUserIds as? List<*>)
            ?.mapNotNull { it as? String }
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            .orEmpty()
    }

    fun isPeerBlocked(
        blockedUserIds: List<String>,
        peerId: String?
    ): Boolean {
        return !peerId.isNullOrBlank() && blockedUserIds.contains(peerId)
    }

    fun hiddenRanges(
        completedRanges: List<MessageHiddenRange>,
        activeRange: MessageHiddenRange?
    ): List<MessageHiddenRange> {
        return completedRanges + listOfNotNull(activeRange)
    }
}
