package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageComposerControllerTest {

    @Test
    fun onTextChanged_trimsTextAndUpdatesTypingAndSendState() {
        val controller = ChatMessageComposerController()

        val change = controller.onTextChanged("  hello  ")

        assertTrue(change.isTyping)
        assertTrue(change.uiState.isSendButtonEnabled)
        assertFalse(change.uiState.isSending)
        assertEquals(
            ChatTextSendDecision.Send("hello"),
            controller.beginTextSend(
                text = "  hello  ",
                isChatBlocked = false
            )
        )
    }

    @Test
    fun onTextChanged_disablesTypingAndSendForBlankText() {
        val controller = ChatMessageComposerController()

        val change = controller.onTextChanged("   ")

        assertFalse(change.isTyping)
        assertFalse(change.uiState.isSendButtonEnabled)
        assertEquals(
            ChatTextSendDecision.Rejected(ChatComposerSendRejection.EMPTY),
            controller.beginTextSend(
                text = "   ",
                isChatBlocked = false
            )
        )
    }

    @Test
    fun beginTextSend_rejectsBlockedConversationWithoutStartingSend() {
        val controller = ChatMessageComposerController()

        val action = controller.beginTextSend(
            text = "hello",
            isChatBlocked = true
        )

        assertEquals(
            ChatTextSendDecision.Rejected(ChatComposerSendRejection.BLOCKED),
            action
        )
        assertFalse(controller.isSending)
        assertTrue(controller.uiState("hello").isSendButtonEnabled)
    }

    @Test
    fun beginTextSend_preventsConcurrentDeliveryUntilSendFinishes() {
        val controller = ChatMessageComposerController()

        val firstAction = controller.beginTextSend(
            text = "hello",
            isChatBlocked = false
        )
        val secondAction = controller.beginTextSend(
            text = "second",
            isChatBlocked = false
        )

        assertEquals(ChatTextSendDecision.Send("hello"), firstAction)
        assertEquals(
            ChatTextSendDecision.Rejected(ChatComposerSendRejection.BUSY),
            secondAction
        )
        assertTrue(controller.isSending)
        assertFalse(controller.uiState("hello").isSendButtonEnabled)

        val finishedState = controller.finishSend("hello")

        assertFalse(finishedState.isSending)
        assertTrue(finishedState.isSendButtonEnabled)
    }

    @Test
    fun beginAttachmentSend_appliesBlockedAndBusyRules() {
        val controller = ChatMessageComposerController()

        assertEquals(
            ChatMediaSendDecision.Rejected(ChatComposerSendRejection.BLOCKED),
            controller.beginAttachmentSend(isChatBlocked = true)
        )
        assertEquals(
            ChatMediaSendDecision.Send,
            controller.beginAttachmentSend(isChatBlocked = false)
        )
        assertEquals(
            ChatMediaSendDecision.Rejected(ChatComposerSendRejection.BUSY),
            controller.beginAttachmentSend(isChatBlocked = false)
        )
    }

    @Test
    fun beginAudioSend_usesTheSameSingleDeliveryGuard() {
        val controller = ChatMessageComposerController()

        assertEquals(ChatMediaSendDecision.Send, controller.beginAudioSend())
        assertEquals(
            ChatMediaSendDecision.Rejected(ChatComposerSendRejection.BUSY),
            controller.beginAudioSend()
        )

        controller.finishSend(text = null)

        assertEquals(ChatMediaSendDecision.Send, controller.beginAudioSend())
    }

    @Test
    fun resolveVoiceToggle_allowsStoppingActiveRecordingWhileSendIsBusy() {
        val controller = ChatMessageComposerController()
        controller.beginAudioSend()

        assertEquals(
            ChatVoiceToggleAction.Busy,
            controller.resolveVoiceToggle(
                isChatBlocked = false,
                isVoiceRecording = false
            )
        )
        assertEquals(
            ChatVoiceToggleAction.Toggle,
            controller.resolveVoiceToggle(
                isChatBlocked = false,
                isVoiceRecording = true
            )
        )
    }

    @Test
    fun resolveVoiceToggle_prioritizesBlockedConversation() {
        val controller = ChatMessageComposerController()

        assertEquals(
            ChatVoiceToggleAction.Blocked,
            controller.resolveVoiceToggle(
                isChatBlocked = true,
                isVoiceRecording = true
            )
        )
    }
}
