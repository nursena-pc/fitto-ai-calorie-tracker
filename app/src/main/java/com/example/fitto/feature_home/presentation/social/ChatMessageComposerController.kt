package com.example.fitto.feature_home.presentation.social

class ChatMessageComposerController {

    var isSending: Boolean = false
        private set

    fun onTextChanged(text: CharSequence?): ChatComposerTextChange {
        val normalizedText = normalizeText(text)

        return ChatComposerTextChange(
            uiState = uiState(normalizedText),
            isTyping = normalizedText.isNotBlank()
        )
    }

    fun uiState(text: CharSequence?): ChatComposerUiState {
        return uiState(normalizeText(text))
    }

    fun beginTextSend(
        text: CharSequence?,
        isChatBlocked: Boolean
    ): ChatTextSendDecision {
        val normalizedText = normalizeText(text)

        commonSendRejection(isChatBlocked)?.let { rejection ->
            return ChatTextSendDecision.Rejected(rejection)
        }
        if (normalizedText.isBlank()) {
            return ChatTextSendDecision.Rejected(ChatComposerSendRejection.EMPTY)
        }

        isSending = true
        return ChatTextSendDecision.Send(normalizedText)
    }

    fun beginAttachmentSend(isChatBlocked: Boolean): ChatMediaSendDecision {
        commonSendRejection(isChatBlocked)?.let { rejection ->
            return ChatMediaSendDecision.Rejected(rejection)
        }

        isSending = true
        return ChatMediaSendDecision.Send
    }

    fun beginAudioSend(): ChatMediaSendDecision {
        if (isSending) {
            return ChatMediaSendDecision.Rejected(ChatComposerSendRejection.BUSY)
        }

        isSending = true
        return ChatMediaSendDecision.Send
    }

    fun resolveVoiceToggle(
        isChatBlocked: Boolean,
        isVoiceRecording: Boolean
    ): ChatVoiceToggleAction {
        return when {
            isChatBlocked -> ChatVoiceToggleAction.Blocked
            isSending && !isVoiceRecording -> ChatVoiceToggleAction.Busy
            else -> ChatVoiceToggleAction.Toggle
        }
    }

    fun finishSend(text: CharSequence?): ChatComposerUiState {
        isSending = false
        return uiState(text)
    }

    private fun uiState(normalizedText: String): ChatComposerUiState {
        return ChatComposerUiState(
            isSending = isSending,
            isSendButtonEnabled = normalizedText.isNotBlank() && !isSending
        )
    }

    private fun commonSendRejection(
        isChatBlocked: Boolean
    ): ChatComposerSendRejection? {
        return when {
            isChatBlocked -> ChatComposerSendRejection.BLOCKED
            isSending -> ChatComposerSendRejection.BUSY
            else -> null
        }
    }

    private fun normalizeText(text: CharSequence?): String {
        return text
            ?.toString()
            ?.trim()
            .orEmpty()
    }
}

data class ChatComposerUiState(
    val isSending: Boolean,
    val isSendButtonEnabled: Boolean
)

data class ChatComposerTextChange(
    val uiState: ChatComposerUiState,
    val isTyping: Boolean
)

sealed interface ChatTextSendDecision {
    data class Send(val text: String) : ChatTextSendDecision

    data class Rejected(
        val reason: ChatComposerSendRejection
    ) : ChatTextSendDecision
}

sealed interface ChatMediaSendDecision {
    data object Send : ChatMediaSendDecision

    data class Rejected(
        val reason: ChatComposerSendRejection
    ) : ChatMediaSendDecision
}

enum class ChatComposerSendRejection {
    BLOCKED,
    BUSY,
    EMPTY
}

sealed interface ChatVoiceToggleAction {
    data object Toggle : ChatVoiceToggleAction

    data object Blocked : ChatVoiceToggleAction

    data object Busy : ChatVoiceToggleAction
}
