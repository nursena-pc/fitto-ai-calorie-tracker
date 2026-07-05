package com.example.fitto.feature_home.presentation.social

internal data class ChatConversationState(
    val thread: MessageThreadUiModel? = null,
    val messages: List<ChatMessageUiModel> = emptyList()
)

/** Stores the latest conversation snapshot across Fragment view recreation. */
internal class ChatConversationStateStore(
    initialState: ChatConversationState = ChatConversationState()
) {
    var state: ChatConversationState = initialState
        private set

    fun updateThread(thread: MessageThreadUiModel) {
        state = state.copy(thread = thread)
    }

    fun updateThreadTitle(title: String) {
        state = state.copy(thread = state.thread?.copy(title = title))
    }

    fun updateThreadPhoto(photoUrl: String?) {
        state = state.copy(thread = state.thread?.copy(photoUrl = photoUrl))
    }

    fun updateMessages(messages: List<ChatMessageUiModel>) {
        state = state.copy(messages = messages)
    }
}
