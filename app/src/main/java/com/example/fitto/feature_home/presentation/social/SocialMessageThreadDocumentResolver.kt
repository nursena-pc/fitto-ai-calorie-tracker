package com.example.fitto.feature_home.presentation.social

internal object SocialMessageThreadDocumentResolver {

    private const val THREAD_TYPE_GROUP = "group"
    private const val DEFAULT_GROUP_TITLE = "Fitto Grup"
    private const val DEFAULT_DIRECT_TITLE = "fitto_user"
    private const val EMPTY_LAST_MESSAGE = "Henüz mesaj yok."

    fun resolve(
        id: String,
        type: String?,
        currentUserId: String,
        participantIds: List<String>,
        participants: Map<*, *>?,
        groupTitle: String?,
        groupPhotoUrl: String?,
        lastMessageText: String?,
        lastMessageCreatedAt: Long?,
        updatedAt: Long?,
        clearedAt: Map<*, *>?,
        unreadCounts: Map<*, *>?,
        memberCount: Int?,
        includeClearedThread: Boolean
    ): MessageThreadUiModel? {
        val isGroup = type == THREAD_TYPE_GROUP
        val otherUserId = participantIds.firstOrNull { participantId -> participantId != currentUserId }
            ?: currentUserId
        val otherParticipant = participants?.get(otherUserId) as? Map<*, *>

        val title = if (isGroup) {
            groupTitle.orEmpty().ifBlank { DEFAULT_GROUP_TITLE }
        } else {
            (otherParticipant?.get("userName") as? String)
                ?.ifBlank { null }
                ?: DEFAULT_DIRECT_TITLE
        }

        val photoUrl = if (isGroup) {
            groupPhotoUrl
        } else {
            otherParticipant?.get("photoUrl") as? String
        }

        val rawLastMessageCreatedAt = lastMessageCreatedAt ?: updatedAt ?: 0L
        val currentUserClearedAt = (clearedAt?.get(currentUserId) as? Number)?.toLong() ?: 0L
        val isClearedForCurrentUser = rawLastMessageCreatedAt > 0L &&
                rawLastMessageCreatedAt <= currentUserClearedAt

        if (isClearedForCurrentUser && !includeClearedThread) {
            return null
        }

        val resolvedLastMessage = if (isClearedForCurrentUser) {
            EMPTY_LAST_MESSAGE
        } else {
            lastMessageText.orEmpty().ifBlank { EMPTY_LAST_MESSAGE }
        }
        val resolvedLastMessageCreatedAt = if (isClearedForCurrentUser) {
            0L
        } else {
            rawLastMessageCreatedAt
        }
        val unreadCount = if (isClearedForCurrentUser) {
            0
        } else {
            (unreadCounts?.get(currentUserId) as? Number)?.toInt() ?: 0
        }

        return MessageThreadUiModel(
            id = id,
            title = title,
            lastMessage = resolvedLastMessage,
            timeText = resolvedLastMessageCreatedAt
                .takeIf { it > 0L }
                ?.let { SocialTextResolver.formatRelativeTime(it) }
                .orEmpty(),
            isGroup = isGroup,
            highlightText = if (unreadCount > 0) "$unreadCount yeni" else null,
            initials = SocialTextResolver.createInitials(title),
            unreadCount = unreadCount,
            participantIds = participantIds,
            otherParticipantId = if (isGroup) null else otherUserId,
            photoUrl = photoUrl,
            memberCount = memberCount ?: participantIds.size,
            lastMessageCreatedAt = resolvedLastMessageCreatedAt
        )
    }
}
