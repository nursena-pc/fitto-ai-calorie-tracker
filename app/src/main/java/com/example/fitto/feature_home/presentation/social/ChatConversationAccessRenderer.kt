package com.example.fitto.feature_home.presentation.social

import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.databinding.FragmentChatDetailBinding

class ChatConversationAccessRenderer(
    private val binding: FragmentChatDetailBinding
) {
    fun render(state: ChatConversationAccessState) {
        when (state.mode) {
            ChatConversationAccessMode.AVAILABLE -> renderAvailableConversation()
            ChatConversationAccessMode.BLOCKED -> renderBlockedConversation()
            ChatConversationAccessMode.DELETED_USER -> renderDeletedUserConversation()
        }
    }

    private fun renderAvailableConversation() = with(binding) {
        chatMessageEditText.hint = root.context.getString(R.string.chat_message_hint)
        chatMessageEditText.isEnabled = true
        chatVoiceButton.isEnabled = true
        chatAttachButton.isEnabled = true
    }

    private fun renderBlockedConversation() = with(binding) {
        chatMessageEditText.setText("")
        chatMessageEditText.hint = root.context.getString(R.string.chat_blocked_input_hint)
        chatMessageEditText.isEnabled = false

        chatSendButton.isEnabled = false
        chatVoiceButton.isEnabled = false
        chatAttachButton.isEnabled = false
    }

    private fun renderDeletedUserConversation() = with(binding) {
        chatTitleText.setText(R.string.deleted_user_display_name)
        chatSubtitleText.setText(R.string.social_deleted_user_unavailable)
        chatMoreButton.isVisible = false

        chatAvatarImage.setImageDrawable(null)
        chatAvatarImage.isVisible = false
        chatGroupIcon.isVisible = false
        chatInitialsText.isVisible = true
        chatInitialsText.text = root.context
            .getString(R.string.deleted_user_display_name)
            .firstOrNull()
            ?.uppercase()
            ?: "S"

        chatMessageEditText.setText("")
        chatMessageEditText.hint = root.context.getString(R.string.social_deleted_user_unavailable)
        chatMessageEditText.isEnabled = false

        chatSendButton.isEnabled = false
        chatVoiceButton.isEnabled = false
        chatAttachButton.isEnabled = false
    }
}
