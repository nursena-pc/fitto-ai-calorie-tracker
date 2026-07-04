package com.example.fitto.feature_home.presentation.social

enum class ChatConversationAccessMode {
    AVAILABLE,
    BLOCKED,
    DELETED_USER
}

data class ChatConversationAccessState(
    val mode: ChatConversationAccessMode,
    val isBlocked: Boolean,
    val isOtherUserDeleted: Boolean,
    val hiddenMessageRanges: List<MessageHiddenRange>
) {
    val isInteractionEnabled: Boolean
        get() = mode == ChatConversationAccessMode.AVAILABLE
}

sealed interface ChatDirectProfileOpenDecision {
    data object MissingParticipant : ChatDirectProfileOpenDecision
    data object DeletedUser : ChatDirectProfileOpenDecision
    data class Open(val userId: String) : ChatDirectProfileOpenDecision
}

data class ChatBlockPreview(
    val text: String,
    val createdAt: Long
)

class ChatConversationAccessController(
    private val isGroup: Boolean
) {
    private var isBlocked: Boolean = false
    private var isOtherUserDeleted: Boolean = false
    private var hiddenMessageRanges: List<MessageHiddenRange> = emptyList()

    val currentState: ChatConversationAccessState
        get() = resolveState()

    fun onBlockStateChanged(
        blockState: DirectThreadBlockState
    ): ChatConversationAccessState {
        if (isGroup) return currentState

        isBlocked = blockState.isBlocked
        hiddenMessageRanges = blockState.hiddenRanges
        return resolveState()
    }

    fun onDeletedUserChanged(
        deleted: Boolean
    ): ChatConversationAccessState {
        if (isGroup) return currentState

        isOtherUserDeleted = deleted
        return resolveState()
    }

    fun resolveDirectProfileOpen(
        otherParticipantId: String?
    ): ChatDirectProfileOpenDecision {
        val userId = otherParticipantId?.takeIf(String::isNotBlank)
            ?: return ChatDirectProfileOpenDecision.MissingParticipant

        if (isOtherUserDeleted) {
            return ChatDirectProfileOpenDecision.DeletedUser
        }

        return ChatDirectProfileOpenDecision.Open(userId)
    }

    fun shouldShowMoreButton(
        hasSelectedMessage: Boolean
    ): Boolean {
        return !isGroup && !isOtherUserDeleted && !hasSelectedMessage
    }

    fun resolveBlockPreview(
        messages: List<ChatMessageUiModel>,
        emptyPreviewText: String,
        fallbackCreatedAt: Long
    ): ChatBlockPreview {
        val lastVisibleMessage = messages.lastOrNull { message ->
            !message.isDeleted
        }

        return ChatBlockPreview(
            text = lastVisibleMessage?.text
                ?.takeIf(String::isNotBlank)
                ?: emptyPreviewText,
            createdAt = lastVisibleMessage?.createdAt
                ?: fallbackCreatedAt
        )
    }

    private fun resolveState(): ChatConversationAccessState {
        val mode = when {
            isOtherUserDeleted -> ChatConversationAccessMode.DELETED_USER
            isBlocked -> ChatConversationAccessMode.BLOCKED
            else -> ChatConversationAccessMode.AVAILABLE
        }

        return ChatConversationAccessState(
            mode = mode,
            isBlocked = isBlocked,
            isOtherUserDeleted = isOtherUserDeleted,
            hiddenMessageRanges = hiddenMessageRanges
        )
    }
}
