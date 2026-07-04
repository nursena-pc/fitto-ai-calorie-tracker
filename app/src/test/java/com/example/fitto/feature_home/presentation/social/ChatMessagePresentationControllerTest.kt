package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessagePresentationControllerTest {

    @Test
    fun present_hidesDirectMessagesInsideHiddenRanges() {
        val controller = ChatMessagePresentationController(highlightMessageId = null)
        val hiddenMessage = message(
            id = "hidden",
            senderId = "other",
            createdAt = 150L
        )
        val visibleMessage = message(
            id = "visible",
            senderId = "me",
            createdAt = 160L
        )

        val presentation = controller.present(
            input(
                messages = listOf(hiddenMessage, visibleMessage),
                hiddenMessageRanges = listOf(
                    MessageHiddenRange(
                        userId = "other",
                        startAt = 100L,
                        endAt = 200L
                    )
                )
            )
        )

        assertEquals(listOf(visibleMessage), presentation.visibleMessages)
    }

    @Test
    fun present_usesBlockedFallbackWhenNoHiddenRangesExist() {
        val controller = ChatMessagePresentationController(highlightMessageId = null)
        val blockedUserMessage = message(
            id = "blocked",
            senderId = "other",
            createdAt = 100L
        )
        val ownMessage = message(
            id = "own",
            senderId = "me",
            createdAt = 200L
        )

        val presentation = controller.present(
            input(
                messages = listOf(blockedUserMessage, ownMessage),
                isCurrentChatBlocked = true
            )
        )

        assertEquals(listOf(ownMessage), presentation.visibleMessages)
    }

    @Test
    fun present_doesNotFilterGroupMessages() {
        val controller = ChatMessagePresentationController(highlightMessageId = null)
        val message = message(
            id = "group-message",
            senderId = "other",
            createdAt = 150L
        )

        val presentation = controller.present(
            input(
                messages = listOf(message),
                isGroup = true,
                isCurrentChatBlocked = true,
                hiddenMessageRanges = listOf(
                    MessageHiddenRange(
                        userId = "other",
                        startAt = 100L,
                        endAt = 200L
                    )
                )
            )
        )

        assertEquals(listOf(message), presentation.visibleMessages)
    }

    @Test
    fun toggleMessageSelection_selectsAndClearsSameMessage() {
        val controller = ChatMessagePresentationController(highlightMessageId = null)
        val message = message(id = "message-1")

        controller.toggleMessageSelection(message)
        assertSame(message, controller.selectedMessage)

        controller.toggleMessageSelection(message)
        assertNull(controller.selectedMessage)
    }

    @Test
    fun present_clearsSelectionWhenSelectedMessageBecomesHidden() {
        val controller = ChatMessagePresentationController(highlightMessageId = null)
        val selectedMessage = message(
            id = "selected",
            senderId = "other",
            createdAt = 150L
        )
        controller.selectMessage(selectedMessage)

        val presentation = controller.present(
            input(
                messages = listOf(selectedMessage),
                hiddenMessageRanges = listOf(
                    MessageHiddenRange(
                        userId = "other",
                        startAt = 100L,
                        endAt = 200L
                    )
                )
            )
        )

        assertTrue(presentation.selectionWasCleared)
        assertNull(controller.selectedMessage)
    }

    @Test
    fun present_highlightsRequestedMessageOnlyOnce() {
        val controller = ChatMessagePresentationController(
            highlightMessageId = "target"
        )
        val messages = listOf(
            message(id = "first"),
            message(id = "target")
        )

        val firstPresentation = controller.present(input(messages = messages))

        assertEquals(
            ChatMessagePostSubmitAction.HighlightMessage(
                messageId = "target",
                position = 1,
                clearAfterMillis = 2_200L
            ),
            firstPresentation.postSubmitAction
        )
        assertTrue(
            controller.shouldApplyPostSubmitAction(firstPresentation.postSubmitAction)
        )
        assertFalse(
            controller.shouldApplyPostSubmitAction(firstPresentation.postSubmitAction)
        )

        val secondPresentation = controller.present(input(messages = messages))

        assertEquals(
            ChatMessagePostSubmitAction.None,
            secondPresentation.postSubmitAction
        )
    }

    @Test
    fun present_keepsHighlightPendingUntilTargetAppears() {
        val controller = ChatMessagePresentationController(
            highlightMessageId = "target"
        )

        val missingPresentation = controller.present(
            input(messages = listOf(message(id = "first")))
        )
        val foundPresentation = controller.present(
            input(messages = listOf(message(id = "first"), message(id = "target")))
        )

        assertEquals(
            ChatMessagePostSubmitAction.None,
            missingPresentation.postSubmitAction
        )
        assertEquals(
            ChatMessagePostSubmitAction.HighlightMessage(
                messageId = "target",
                position = 1,
                clearAfterMillis = 2_200L
            ),
            foundPresentation.postSubmitAction
        )
        assertTrue(
            controller.shouldApplyPostSubmitAction(foundPresentation.postSubmitAction)
        )
    }

    @Test
    fun present_scrollsToLatestMessageWhenNoHighlightWasRequested() {
        val controller = ChatMessagePresentationController(highlightMessageId = null)

        val presentation = controller.present(
            input(
                messages = listOf(
                    message(id = "first"),
                    message(id = "second")
                )
            )
        )

        assertFalse(presentation.selectionWasCleared)
        assertEquals(
            ChatMessagePostSubmitAction.ScrollToPosition(position = 1),
            presentation.postSubmitAction
        )
    }

    private fun input(
        messages: List<ChatMessageUiModel>,
        isGroup: Boolean = false,
        hiddenSenderId: String? = "other",
        isCurrentChatBlocked: Boolean = false,
        hiddenMessageRanges: List<MessageHiddenRange> = emptyList()
    ): ChatMessagePresentationInput {
        return ChatMessagePresentationInput(
            messages = messages,
            isGroup = isGroup,
            hiddenSenderId = hiddenSenderId,
            isCurrentChatBlocked = isCurrentChatBlocked,
            hiddenMessageRanges = hiddenMessageRanges
        )
    }

    private fun message(
        id: String,
        senderId: String = "me",
        createdAt: Long = 0L
    ): ChatMessageUiModel {
        return ChatMessageUiModel(
            id = id,
            text = id,
            timeText = "12:00",
            senderId = senderId,
            senderName = senderId,
            isFromMe = senderId == "me",
            createdAt = createdAt
        )
    }
}
