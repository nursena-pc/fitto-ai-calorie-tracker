package com.example.fitto.feature_home.presentation.social

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemChatMessageBinding

class ChatMessageAdapter(
    private val onMessageLongClick: (ChatMessageUiModel) -> Unit = {},
    private val onMessageClick: (ChatMessageUiModel) -> Unit = {},
    private val onAttachmentClick: (ChatMessageUiModel) -> Unit = {},
    private val onAudioClick: (String) -> Unit = {}
) : ListAdapter<ChatMessageUiModel, ChatMessageAdapter.ChatMessageViewHolder>(DiffCallback) {

    private var selectedMessageId: String? = null
    private var highlightedMessageId: String? = null
    private var playingAudioUrl: String? = null
    fun setSelectedMessage(messageId: String?) {
        selectedMessageId = messageId
        notifyDataSetChanged()
    }

    fun setHighlightedMessage(messageId: String?) {
        highlightedMessageId = messageId
        notifyDataSetChanged()
    }
    fun setPlayingAudioUrl(url: String?) {
        playingAudioUrl = url
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatMessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(
            message = item,
            isSelected = selectedMessageId == item.id,
            isHighlighted = highlightedMessageId == item.id
        )
    }

    inner class ChatMessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            message: ChatMessageUiModel,
            isSelected: Boolean,
            isHighlighted: Boolean
        ) = with(binding) {
            val context = root.context
            val deletedUserName = context.getString(R.string.deleted_user_display_name)
            val senderIsDeleted = message.isDeleted || message.senderName == deletedUserName

            val isImageMessage = message.isImageMessage()
            val isAudioMessage = message.isAudioMessage()
            val isFileMessage = message.type == "file" && !isImageMessage
            val isPlayingThisAudio = isAudioMessage && message.fileUrl == playingAudioUrl

            chatMessageRoot.gravity = if (message.isFromMe) Gravity.END else Gravity.START
            chatMessageRow.gravity = if (message.isFromMe) Gravity.END else Gravity.START

            chatMessageRoot.background = when {
                isSelected -> ContextCompat.getDrawable(context, R.drawable.bg_chat_message_selected)
                isHighlighted -> ContextCompat.getDrawable(context, R.drawable.bg_message_highlight)
                else -> null
            }

            chatSenderText.isVisible = !message.isFromMe
            chatSenderText.text = if (senderIsDeleted) {
                deletedUserName
            } else {
                message.senderName
            }

            chatMessageImagePreview.isVisible = isImageMessage

            if (isImageMessage) {
                chatMessageImagePreview.load(message.fileUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_circle_primary_soft)
                    error(R.drawable.bg_circle_primary_soft)
                }
            } else {
                chatMessageImagePreview.setImageDrawable(null)
            }

            val displayText = when {
                message.isDeleted -> message.text

                isAudioMessage -> {
                    val audioIcon = if (isPlayingThisAudio) "⏸" else "▶"

                    "$audioIcon ${context.getString(R.string.chat_voice_message)} · ${
                        formatAudioDuration(message.audioDurationMillis)
                    }"
                }

                isImageMessage -> {
                    ""
                }

                isFileMessage -> {
                    "📎 ${message.fileName ?: message.text.ifBlank { "Dosya" }}"
                }

                else -> {
                    message.text
                }
            }

            chatMessageBubbleText.text = displayText
            chatMessageBubbleText.isVisible = displayText.isNotBlank()

            if (displayText.isNotBlank() && !message.isDeleted) {
                chatMessageBubbleText.linksClickable = true
                chatMessageBubbleText.autoLinkMask = Linkify.WEB_URLS
                chatMessageBubbleText.movementMethod = LinkMovementMethod.getInstance()
                Linkify.addLinks(chatMessageBubbleText, Linkify.WEB_URLS)

                chatMessageBubbleText.setLinkTextColor(
                    ContextCompat.getColor(
                        context,
                        if (message.isFromMe) {
                            R.color.white
                        } else {
                            R.color.fitto_green
                        }
                    )
                )
            }

            chatMessageBubbleText.setTypeface(
                null,
                when {
                    message.isDeleted -> Typeface.ITALIC
                    isPlayingThisAudio -> Typeface.BOLD
                    else -> Typeface.NORMAL
                }
            )

            chatMessageBubbleText.background = ContextCompat.getDrawable(
                context,
                if (message.isFromMe) {
                    R.drawable.bg_chat_bubble_sent
                } else {
                    R.drawable.bg_chat_bubble_received
                }
            )

            chatMessageBubbleText.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (message.isFromMe) {
                        R.color.white
                    } else {
                        R.color.fitto_text_dark
                    }
                )
            )
            chatMessageBubbleText.setLinkTextColor(
                ContextCompat.getColor(
                    context,
                    if (message.isFromMe) {
                        R.color.white
                    } else {
                        R.color.fitto_green
                    }
                )
            )
            chatMessageTimeText.text = message.timeText

            chatMessageAvatarContainer.isVisible = !message.isFromMe

            val hasPhoto = !message.senderPhotoUrl.isNullOrBlank() && !senderIsDeleted
            chatMessageAvatarImage.isVisible = !message.isFromMe && hasPhoto
            chatMessageAvatarInitialText.isVisible = !message.isFromMe && !hasPhoto
            chatMessageAvatarInitialText.text =
                if (senderIsDeleted) {
                    deletedUserName.firstOrNull()?.uppercase() ?: "S"
                } else {
                    message.senderName.firstOrNull()?.uppercase() ?: "F"
                }

            if (!message.isFromMe && hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = chatMessageAvatarImage,
                    photoUrl = message.senderPhotoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                chatMessageAvatarImage.setImageDrawable(null)
            }

            chatMessageMetaRow.gravity = if (message.isFromMe) Gravity.END else Gravity.START
            chatMessageTimeText.gravity = if (message.isFromMe) Gravity.END else Gravity.START

            chatMessageReadReceiptText.isVisible =
                message.isFromMe &&
                        !message.isDeleted &&
                        !message.readReceiptText.isNullOrBlank()

            chatMessageReadReceiptText.text = message.readReceiptText.orEmpty()

            val messageClickListener = android.view.View.OnClickListener {
                val fileUrl = message.fileUrl

                when {
                    message.isDeleted -> {
                        onMessageClick(message)
                    }

                    isAudioMessage && !fileUrl.isNullOrBlank() -> {
                        onAudioClick(fileUrl)
                    }

                    !fileUrl.isNullOrBlank() -> {
                        onAttachmentClick(message)
                    }

                    else -> {
                        onMessageClick(message)
                    }
                }
            }

            root.setOnClickListener(messageClickListener)
            chatMessageBubbleText.setOnClickListener(messageClickListener)
            chatMessageImagePreview.setOnClickListener(messageClickListener)

            root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    private fun ChatMessageUiModel.isImageMessage(): Boolean {
        return !fileUrl.isNullOrBlank() &&
                fileMimeType?.startsWith("image/") == true
    }
    private fun ChatMessageUiModel.isAudioMessage(): Boolean {
        return !fileUrl.isNullOrBlank() &&
                (
                        type == "audio" ||
                                fileMimeType?.startsWith("audio/") == true
                        )
    }

    private fun formatAudioDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatMessageUiModel>() {
        override fun areItemsTheSame(
            oldItem: ChatMessageUiModel,
            newItem: ChatMessageUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ChatMessageUiModel,
            newItem: ChatMessageUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}