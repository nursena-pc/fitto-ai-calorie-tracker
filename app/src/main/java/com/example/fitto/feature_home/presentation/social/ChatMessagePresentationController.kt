package com.example.fitto.feature_home.presentation.social

internal data class ChatMessagePresentationInput(
    val messages: List<ChatMessageUiModel>,
    val isGroup: Boolean,
    val hiddenSenderId: String?,
    val isCurrentChatBlocked: Boolean,
    val hiddenMessageRanges: List<MessageHiddenRange>
)

internal data class ChatMessagePresentation(
    val visibleMessages: List<ChatMessageUiModel>,
    val selectionWasCleared: Boolean,
    val postSubmitAction: ChatMessagePostSubmitAction
)

internal sealed interface ChatMessagePostSubmitAction {
    data object None : ChatMessagePostSubmitAction

    data class ScrollToPosition(
        val position: Int
    ) : ChatMessagePostSubmitAction

    data class HighlightMessage(
        val messageId: String,
        val position: Int,
        val clearAfterMillis: Long
    ) : ChatMessagePostSubmitAction
}

internal class ChatMessagePresentationController(
    highlightMessageId: String?
) {
    private val pendingHighlightMessageId = highlightMessageId
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    private var hasHandledHighlightMessage = false

    var selectedMessage: ChatMessageUiModel? = null
        private set

    fun selectMessage(message: ChatMessageUiModel) {
        selectedMessage = message
    }

    fun toggleMessageSelection(message: ChatMessageUiModel) {
        selectedMessage = if (selectedMessage?.id == message.id) {
            null
        } else {
            message
        }
    }

    fun clearSelection() {
        selectedMessage = null
    }

    fun shouldApplyPostSubmitAction(action: ChatMessagePostSubmitAction): Boolean {
        if (action !is ChatMessagePostSubmitAction.HighlightMessage) return true
        if (hasHandledHighlightMessage) return false

        hasHandledHighlightMessage = true
        return true
    }

    fun present(input: ChatMessagePresentationInput): ChatMessagePresentation {
        val visibleMessages = input.messages.filterNot { message ->
            shouldHideMessage(
                message = message,
                input = input
            )
        }

        val selectionWasCleared = clearSelectionWhenMessageIsHidden(visibleMessages)
        val postSubmitAction = resolvePostSubmitAction(visibleMessages)

        return ChatMessagePresentation(
            visibleMessages = visibleMessages,
            selectionWasCleared = selectionWasCleared,
            postSubmitAction = postSubmitAction
        )
    }

    private fun shouldHideMessage(
        message: ChatMessageUiModel,
        input: ChatMessagePresentationInput
    ): Boolean {
        val hiddenSenderId = input.hiddenSenderId
        if (input.isGroup || hiddenSenderId.isNullOrBlank()) return false
        if (message.senderId != hiddenSenderId) return false

        val isInsideHiddenRange = input.hiddenMessageRanges.any { range ->
            range.userId == message.senderId &&
                    message.createdAt >= range.startAt &&
                    (range.endAt == null || message.createdAt <= range.endAt)
        }
        val shouldUseBlockedFallback = input.isCurrentChatBlocked &&
                input.hiddenMessageRanges.isEmpty()

        return isInsideHiddenRange || shouldUseBlockedFallback
    }

    private fun clearSelectionWhenMessageIsHidden(
        visibleMessages: List<ChatMessageUiModel>
    ): Boolean {
        val selectedId = selectedMessage?.id ?: return false
        if (visibleMessages.any { message -> message.id == selectedId }) return false

        selectedMessage = null
        return true
    }

    private fun resolvePostSubmitAction(
        visibleMessages: List<ChatMessageUiModel>
    ): ChatMessagePostSubmitAction {
        val targetId = pendingHighlightMessageId

        if (targetId != null && !hasHandledHighlightMessage) {
            val targetIndex = visibleMessages.indexOfFirst { message ->
                message.id == targetId
            }

            if (targetIndex >= 0) {
                return ChatMessagePostSubmitAction.HighlightMessage(
                    messageId = targetId,
                    position = targetIndex,
                    clearAfterMillis = HIGHLIGHT_DURATION_MILLIS
                )
            }
        }

        return if (targetId == null && visibleMessages.isNotEmpty()) {
            ChatMessagePostSubmitAction.ScrollToPosition(
                position = visibleMessages.lastIndex
            )
        } else {
            ChatMessagePostSubmitAction.None
        }
    }

    private companion object {
        const val HIGHLIGHT_DURATION_MILLIS = 2_200L
    }
}
