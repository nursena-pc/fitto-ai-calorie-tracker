package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesScreenStateStoreTest {

    @Test
    fun visibleThreads_appliesUnreadFilterAndSearchTogether() {
        val store = MessagesScreenStateStore()
        store.updateThreads(
            listOf(
                thread(id = "1", title = "Ayşe", unreadCount = 2),
                thread(id = "2", title = "Ayşe", unreadCount = 0),
                thread(id = "3", title = "Mehmet", unreadCount = 3)
            )
        )
        store.selectFilter(MessageThreadFilter.UNREAD)
        store.updateSearchQuery("ayşe")

        assertEquals(listOf("1"), store.visibleThreads().map { it.id })
    }

    @Test
    fun visibleThreads_matchesAiAssistantByIdTitleOrLocalizedTitle() {
        val store = MessagesScreenStateStore()
        store.updateThreads(
            listOf(
                thread(id = "ai_thread", title = "Coach"),
                thread(id = "2", title = "AI Coach"),
                thread(id = "3", title = "Beslenme Asistanı"),
                thread(id = "4", title = "Arkadaş")
            )
        )
        store.selectFilter(MessageThreadFilter.AI_ASSISTANT)

        assertEquals(listOf("ai_thread", "2", "3"), store.visibleThreads().map { it.id })
    }

    @Test
    fun beginDeletion_hidesThreadUntilDeletionFails() {
        val store = MessagesScreenStateStore()
        store.updateThreads(listOf(thread(id = "thread-1")))

        store.beginDeletion(threadId = "thread-1", deletedAt = 100L)
        assertTrue(store.visibleThreads().isEmpty())

        store.rollbackDeletion("thread-1")
        assertEquals(listOf("thread-1"), store.visibleThreads().map { it.id })
    }

    @Test
    fun updateThreads_revivesDeletedThreadWhenNewerMessageArrives() {
        val store = MessagesScreenStateStore()
        store.beginDeletion(threadId = "thread-1", deletedAt = 100L)

        store.updateThreads(listOf(thread(id = "thread-1", lastMessageCreatedAt = 101L)))

        assertEquals(listOf("thread-1"), store.visibleThreads().map { it.id })
        assertFalse(store.state.pendingDeletedAtByThreadId.containsKey("thread-1"))
    }

    @Test
    fun updateThreads_keepsDeletedThreadHiddenForOlderSnapshot() {
        val store = MessagesScreenStateStore()
        store.beginDeletion(threadId = "thread-1", deletedAt = 100L)

        store.updateThreads(listOf(thread(id = "thread-1", lastMessageCreatedAt = 100L)))

        assertTrue(store.visibleThreads().isEmpty())
        assertTrue(store.state.pendingDeletedAtByThreadId.containsKey("thread-1"))
    }

    @Test
    fun state_preservesSelectedFilterAndNormalizedSearchQuery() {
        val store = MessagesScreenStateStore()

        store.selectFilter(MessageThreadFilter.GROUPS)
        store.updateSearchQuery("  koşu  ")

        assertEquals(MessageThreadFilter.GROUPS, store.state.selectedFilter)
        assertEquals("koşu", store.state.searchQuery)
    }

    private fun thread(
        id: String,
        title: String = "Thread",
        unreadCount: Int = 0,
        isGroup: Boolean = false,
        lastMessageCreatedAt: Long = 0L
    ): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = id,
            title = title,
            lastMessage = "Son mesaj",
            timeText = "şimdi",
            isGroup = isGroup,
            initials = "T",
            unreadCount = unreadCount,
            lastMessageCreatedAt = lastMessageCreatedAt
        )
    }
}
