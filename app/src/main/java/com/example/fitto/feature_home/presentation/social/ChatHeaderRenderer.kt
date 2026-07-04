package com.example.fitto.feature_home.presentation.social

import androidx.core.view.isVisible
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.FragmentChatDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class ChatHeaderRenderer(
    private val binding: FragmentChatDetailBinding
) {

    fun renderIdentity(presentation: ChatHeaderPresentation) {
        renderTitle(presentation)
        binding.chatInitialsText.text = presentation.initials
        renderAvatar(presentation)
    }

    fun renderTitle(presentation: ChatHeaderPresentation) {
        binding.chatTitleText.text = presentation.title
    }

    fun renderAvatar(presentation: ChatHeaderPresentation) = with(binding) {
        chatAvatarImage.isVisible = presentation.avatarMode == ChatHeaderAvatarMode.PHOTO
        chatGroupIcon.isVisible = presentation.avatarMode == ChatHeaderAvatarMode.GROUP_ICON
        chatInitialsText.isVisible = presentation.avatarMode == ChatHeaderAvatarMode.INITIALS

        if (presentation.avatarMode == ChatHeaderAvatarMode.PHOTO) {
            chatAvatarImage.load(presentation.photoUrl)
        } else {
            chatAvatarImage.setImageDrawable(null)
        }
    }

    fun renderStatus(presentation: ChatHeaderPresentation) = with(binding) {
        val typingIndicator = presentation.typingIndicator
        chatTypingIndicatorContainer.isVisible = typingIndicator != null

        if (typingIndicator != null) {
            val hasPhoto = !typingIndicator.photoUrl.isNullOrBlank()
            chatTypingAvatarImage.isVisible = hasPhoto
            chatTypingAvatarInitialText.isVisible = !hasPhoto
            chatTypingAvatarInitialText.text = typingIndicator.initials

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = chatTypingAvatarImage,
                    photoUrl = typingIndicator.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                chatTypingAvatarImage.setImageDrawable(null)
            }
        }

        renderSubtitle(presentation.subtitle)
    }

    fun renderSubtitle(subtitle: ChatHeaderSubtitle) {
        val text = when (subtitle) {
            is ChatHeaderSubtitle.GroupMembers -> {
                binding.root.context.getString(
                    R.string.chat_group_members,
                    subtitle.memberCount
                )
            }

            ChatHeaderSubtitle.Online -> {
                binding.root.context.getString(R.string.chat_online)
            }

            ChatHeaderSubtitle.Offline -> {
                binding.root.context.getString(R.string.chat_offline)
            }

            is ChatHeaderSubtitle.LastSeen -> {
                val timeText = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
                    .format(Date(subtitle.lastChanged))
                binding.root.context.getString(R.string.chat_last_seen, timeText)
            }

            is ChatHeaderSubtitle.DirectTyping -> {
                binding.root.context.getString(R.string.chat_typing, subtitle.dots)
            }

            is ChatHeaderSubtitle.GroupTypingOne -> {
                binding.root.context.getString(
                    R.string.chat_group_typing_one,
                    subtitle.userName,
                    subtitle.dots
                )
            }

            is ChatHeaderSubtitle.GroupTypingMany -> {
                binding.root.context.getString(
                    R.string.chat_group_typing_many,
                    subtitle.userCount,
                    subtitle.dots
                )
            }
        }

        binding.chatTypingText.text = text
        binding.chatSubtitleText.text = text
    }
}
