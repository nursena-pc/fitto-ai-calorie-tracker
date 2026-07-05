package com.example.fitto.feature_home.presentation.social

internal enum class MessageThreadFilter {
    ALL,
    UNREAD,
    AI_ASSISTANT,
    GROUPS
}

internal data class MessagesScreenState(
    val selectedFilter: MessageThreadFilter = MessageThreadFilter.ALL,
    val searchQuery: String = "",
    val allThreads: List<MessageThreadUiModel> = emptyList(),
    val pendingDeletedAtByThreadId: Map<String, Long> = emptyMap()
)

/** Keeps message-list state independent from the Fragment view lifecycle. */
internal class MessagesScreenStateStore(
    initialState: MessagesScreenState = MessagesScreenState()
) {
    var state: MessagesScreenState = initialState
        private set

    fun selectFilter(filter: MessageThreadFilter) {
        state = state.copy(selectedFilter = filter)
    }

    fun updateSearchQuery(query: String) {
        state = state.copy(searchQuery = query.trim())
    }

    fun updateThreads(threads: List<MessageThreadUiModel>) {
        val revivedThreadIds = threads
            .asSequence()
            .filter { thread ->
                val deletedAt = state.pendingDeletedAtByThreadId[thread.id]
                deletedAt != null && thread.lastMessageCreatedAt > deletedAt
            }
            .map { thread -> thread.id }
            .toSet()

        state = state.copy(
            allThreads = threads,
            pendingDeletedAtByThreadId = state.pendingDeletedAtByThreadId - revivedThreadIds
        )
    }

    fun beginDeletion(threadId: String, deletedAt: Long) {
        state = state.copy(
            pendingDeletedAtByThreadId = state.pendingDeletedAtByThreadId + (threadId to deletedAt)
        )
    }

    fun rollbackDeletion(threadId: String) {
        state = state.copy(
            pendingDeletedAtByThreadId = state.pendingDeletedAtByThreadId - threadId
        )
    }

    fun visibleThreads(): List<MessageThreadUiModel> {
        val current = state
        return current.allThreads
            .asSequence()
            .filterNot { thread -> current.pendingDeletedAtByThreadId.containsKey(thread.id) }
            .filter { thread -> matchesFilter(thread, current.selectedFilter) }
            .filter { thread -> matchesSearch(thread, current.searchQuery) }
            .toList()
    }

    private fun matchesFilter(
        thread: MessageThreadUiModel,
        filter: MessageThreadFilter
    ): Boolean {
        return when (filter) {
            MessageThreadFilter.ALL -> true
            MessageThreadFilter.UNREAD -> thread.unreadCount > 0
            MessageThreadFilter.AI_ASSISTANT -> {
                thread.id.contains("ai", ignoreCase = true) ||
                    thread.title.contains("ai", ignoreCase = true) ||
                    thread.title.contains("asistan", ignoreCase = true)
            }
            MessageThreadFilter.GROUPS -> thread.isGroup
        }
    }

    private fun matchesSearch(thread: MessageThreadUiModel, query: String): Boolean {
        return query.isBlank() ||
            thread.title.contains(query, ignoreCase = true) ||
            thread.lastMessage.contains(query, ignoreCase = true)
    }
}
