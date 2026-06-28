package com.example.fitto.feature_home.presentation.social

internal data class SocialMessageThreadListItem(
    val thread: MessageThreadUiModel,
    val updatedAt: Long
)

internal object SocialMessageThreadListResolver {

    private const val BLOCKED_USER_LAST_MESSAGE = "Bu kullanıcı engellendi"

    fun shouldSkipThreadList(currentUserId: String?): Boolean {
        return currentUserId.isNullOrBlank()
    }

    fun blockedUserIds(value: Any?): Set<String> {
        return (value as? List<*>)
            .orEmpty()
            .mapNotNull { item -> (item as? String)?.trim()?.takeIf { it.isNotBlank() } }
            .toSet()
    }

    fun resolveThreads(
        items: List<SocialMessageThreadListItem>,
        blockedUserIds: Set<String>,
        blockedThreadPreviews: Map<String, BlockedThreadPreview>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MessageThreadUiModel> {
        return items
            .map { item ->
                item.copy(
                    thread = resolveBlockedPreview(
                        thread = item.thread,
                        blockedUserIds = blockedUserIds,
                        blockedThreadPreviews = blockedThreadPreviews,
                        nowMillis = nowMillis
                    )
                )
            }
            .sortedByDescending { item -> item.updatedAt }
            .map { item -> item.thread }
    }

    fun resolveBlockedPreview(
        thread: MessageThreadUiModel,
        blockedUserIds: Set<String>,
        blockedThreadPreviews: Map<String, BlockedThreadPreview>,
        nowMillis: Long = System.currentTimeMillis()
    ): MessageThreadUiModel {
        if (!isBlockedDirectThread(thread, blockedUserIds)) return thread

        val frozenPreview = blockedThreadPreviews[thread.id]

        return thread.copy(
            lastMessage = frozenPreview?.text ?: BLOCKED_USER_LAST_MESSAGE,
            timeText = frozenPreview
                ?.createdAt
                ?.takeIf { it > 0L }
                ?.let { createdAt -> SocialTextResolver.formatRelativeTime(createdAt, nowMillis) }
                .orEmpty(),
            unreadCount = 0,
            highlightText = null
        )
    }

    fun isBlockedDirectThread(
        thread: MessageThreadUiModel,
        blockedUserIds: Set<String>
    ): Boolean {
        return !thread.isGroup &&
                !thread.otherParticipantId.isNullOrBlank() &&
                blockedUserIds.contains(thread.otherParticipantId)
    }
}
