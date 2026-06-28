package com.example.fitto.feature_home.presentation.social

internal object SocialDirectThreadBlockStateResolver {

    fun empty(): DirectThreadBlockState {
        return DirectThreadBlockState(
            isBlocked = false,
            hiddenRanges = emptyList()
        )
    }

    fun isBlocked(
        blockedUserIds: Any?,
        targetUserId: String
    ): Boolean {
        if (targetUserId.isBlank()) return false
        return readBlockedUserIds(blockedUserIds).contains(targetUserId)
    }

    fun resolve(
        blockedUserIds: Any?,
        targetUserId: String,
        completedRanges: List<MessageHiddenRange>,
        activeRange: MessageHiddenRange?
    ): DirectThreadBlockState {
        return DirectThreadBlockState(
            isBlocked = isBlocked(blockedUserIds, targetUserId),
            hiddenRanges = completedRanges + listOfNotNull(activeRange)
        )
    }

    private fun readBlockedUserIds(value: Any?): List<String> {
        return (value as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }
}
