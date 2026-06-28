package com.example.fitto.feature_home.presentation.social

internal object SocialUnreadMessageCountResolver {

    fun unreadCountForThread(
        userId: String,
        unreadCounts: Any?,
        lastMessageHiddenFor: List<String>,
        lastMessageCreatedAt: Long,
        clearedAt: Any?
    ): Int {
        val rawUnreadCount = unreadCountForUser(
            userId = userId,
            unreadCounts = unreadCounts
        )
        val currentUserClearedAt = clearedAtForUser(
            userId = userId,
            clearedAt = clearedAt
        )

        return if (
            lastMessageHiddenFor.contains(userId) ||
            lastMessageCreatedAt <= currentUserClearedAt
        ) {
            0
        } else {
            rawUnreadCount
        }
    }

    private fun unreadCountForUser(
        userId: String,
        unreadCounts: Any?
    ): Int {
        val unreadCountsMap = unreadCounts as? Map<*, *> ?: return 0
        return (unreadCountsMap[userId] as? Number)?.toInt() ?: 0
    }

    private fun clearedAtForUser(
        userId: String,
        clearedAt: Any?
    ): Long {
        val clearedAtMap = clearedAt as? Map<*, *> ?: return 0L
        return (clearedAtMap[userId] as? Number)?.toLong() ?: 0L
    }
}
