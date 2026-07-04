package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatConversationAccessControllerTest {

    @Test
    fun currentState_startsAvailableForDirectConversation() {
        val controller = ChatConversationAccessController(isGroup = false)

        val state = controller.currentState

        assertEquals(ChatConversationAccessMode.AVAILABLE, state.mode)
        assertFalse(state.isBlocked)
        assertFalse(state.isOtherUserDeleted)
        assertTrue(state.isInteractionEnabled)
        assertTrue(state.hiddenMessageRanges.isEmpty())
    }

    @Test
    fun onBlockStateChanged_preservesBlockFlagAndHiddenRanges() {
        val controller = ChatConversationAccessController(isGroup = false)
        val hiddenRange = MessageHiddenRange(
            userId = "user-2",
            startAt = 100L,
            endAt = 200L
        )

        val state = controller.onBlockStateChanged(
            DirectThreadBlockState(
                isBlocked = true,
                hiddenRanges = listOf(hiddenRange)
            )
        )

        assertEquals(ChatConversationAccessMode.BLOCKED, state.mode)
        assertTrue(state.isBlocked)
        assertFalse(state.isInteractionEnabled)
        assertEquals(listOf(hiddenRange), state.hiddenMessageRanges)
    }

    @Test
    fun deletedUser_takesPriorityOverBlockStateAndRemainsRestrictedAfterUnblock() {
        val controller = ChatConversationAccessController(isGroup = false)
        controller.onBlockStateChanged(
            DirectThreadBlockState(
                isBlocked = true,
                hiddenRanges = emptyList()
            )
        )

        val deletedState = controller.onDeletedUserChanged(true)
        val unblockedState = controller.onBlockStateChanged(
            DirectThreadBlockState(
                isBlocked = false,
                hiddenRanges = emptyList()
            )
        )

        assertEquals(ChatConversationAccessMode.DELETED_USER, deletedState.mode)
        assertTrue(deletedState.isOtherUserDeleted)
        assertEquals(ChatConversationAccessMode.DELETED_USER, unblockedState.mode)
        assertFalse(unblockedState.isBlocked)
        assertFalse(unblockedState.isInteractionEnabled)
    }

    @Test
    fun groupConversation_ignoresDirectBlockAndDeletedUserUpdates() {
        val controller = ChatConversationAccessController(isGroup = true)

        controller.onBlockStateChanged(
            DirectThreadBlockState(
                isBlocked = true,
                hiddenRanges = listOf(
                    MessageHiddenRange(
                        userId = "user-2",
                        startAt = 10L,
                        endAt = null
                    )
                )
            )
        )
        val state = controller.onDeletedUserChanged(true)

        assertEquals(ChatConversationAccessMode.AVAILABLE, state.mode)
        assertFalse(state.isBlocked)
        assertFalse(state.isOtherUserDeleted)
        assertTrue(state.hiddenMessageRanges.isEmpty())
    }

    @Test
    fun resolveDirectProfileOpen_handlesMissingDeletedAndAvailableParticipants() {
        val controller = ChatConversationAccessController(isGroup = false)

        assertEquals(
            ChatDirectProfileOpenDecision.MissingParticipant,
            controller.resolveDirectProfileOpen("  ")
        )
        assertEquals(
            ChatDirectProfileOpenDecision.Open("user-2"),
            controller.resolveDirectProfileOpen("user-2")
        )

        controller.onDeletedUserChanged(true)

        assertEquals(
            ChatDirectProfileOpenDecision.DeletedUser,
            controller.resolveDirectProfileOpen("user-2")
        )
    }

    @Test
    fun shouldShowMoreButton_hidesForSelectionDeletedUserAndGroupConversation() {
        val directController = ChatConversationAccessController(isGroup = false)
        val groupController = ChatConversationAccessController(isGroup = true)

        assertTrue(directController.shouldShowMoreButton(hasSelectedMessage = false))
        assertFalse(directController.shouldShowMoreButton(hasSelectedMessage = true))
        assertFalse(groupController.shouldShowMoreButton(hasSelectedMessage = false))

        directController.onDeletedUserChanged(true)

        assertFalse(directController.shouldShowMoreButton(hasSelectedMessage = false))
    }

    @Test
    fun resolveBlockPreview_usesLatestNonDeletedMessage() {
        val controller = ChatConversationAccessController(isGroup = false)
        val messages = listOf(
            message(id = "first", text = "İlk mesaj", createdAt = 100L),
            message(id = "deleted", text = "Silinen", createdAt = 200L, isDeleted = true),
            message(id = "latest", text = "Son mesaj", createdAt = 300L)
        )

        val preview = controller.resolveBlockPreview(
            messages = messages,
            emptyPreviewText = "Henüz mesaj yok.",
            fallbackCreatedAt = 999L
        )

        assertEquals(ChatBlockPreview("Son mesaj", 300L), preview)
    }

    @Test
    fun resolveBlockPreview_usesFallbackForDeletedOrBlankMessages() {
        val controller = ChatConversationAccessController(isGroup = false)
        val messages = listOf(
            message(id = "blank", text = "   ", createdAt = 400L),
            message(id = "deleted", text = "Silinen", createdAt = 500L, isDeleted = true)
        )

        val preview = controller.resolveBlockPreview(
            messages = messages,
            emptyPreviewText = "Henüz mesaj yok.",
            fallbackCreatedAt = 999L
        )

        assertEquals(ChatBlockPreview("Henüz mesaj yok.", 400L), preview)
    }

    private fun message(
        id: String,
        text: String,
        createdAt: Long,
        isDeleted: Boolean = false
    ): ChatMessageUiModel {
        return ChatMessageUiModel(
            id = id,
            text = text,
            timeText = "",
            senderId = "user-2",
            senderName = "Fitto",
            isFromMe = false,
            createdAt = createdAt,
            isDeleted = isDeleted
        )
    }
}
