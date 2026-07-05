package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatConversationStateStoreTest {

    @Test
    fun updateThreadTitleAndPhoto_preservesRemainingThreadFields() {
        val store = ChatConversationStateStore()
        store.updateThread(thread(title = "Eski", photoUrl = "old"))

        store.updateThreadTitle("Yeni")
        store.updateThreadPhoto("new")

        assertEquals("thread-1", store.state.thread?.id)
        assertEquals("Yeni", store.state.thread?.title)
        assertEquals("new", store.state.thread?.photoUrl)
    }

    @Test
    fun updateThreadMetadata_withoutThread_keepsStateEmpty() {
        val store = ChatConversationStateStore()

        store.updateThreadTitle("Yeni")
        store.updateThreadPhoto("photo")

        assertNull(store.state.thread)
    }

    @Test
    fun updateMessages_replacesConversationSnapshot() {
        val store = ChatConversationStateStore()
        val first = message(id = "1", text = "İlk")
        val second = message(id = "2", text = "İkinci")

        store.updateMessages(listOf(first))
        store.updateMessages(listOf(first, second))

        assertEquals(listOf("1", "2"), store.state.messages.map { it.id })
    }

    private fun thread(title: String, photoUrl: String?): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = "thread-1",
            title = title,
            lastMessage = "",
            timeText = "",
            isGroup = true,
            initials = "T",
            photoUrl = photoUrl,
            memberCount = 3
        )
    }

    private fun message(id: String, text: String): ChatMessageUiModel {
        return ChatMessageUiModel(
            id = id,
            text = text,
            timeText = "şimdi",
            senderId = "user",
            senderName = "User",
            isFromMe = false
        )
    }
}
