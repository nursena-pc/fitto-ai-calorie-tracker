package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemMessageThreadBinding

class MessageThreadAdapter(
    private val onThreadClick: (MessageThreadUiModel) -> Unit
) : ListAdapter<MessageThreadUiModel, MessageThreadAdapter.MessageThreadViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageThreadViewHolder {
        val binding = ItemMessageThreadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MessageThreadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageThreadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageThreadViewHolder(
        private val binding: ItemMessageThreadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(thread: MessageThreadUiModel) = with(binding) {
            val context = root.context

            threadTitleText.text = thread.title
            threadLastMessageText.text = thread.lastMessage
            threadTimeText.text = thread.timeText

            threadHighlightText.isVisible = !thread.highlightText.isNullOrBlank()
            threadHighlightText.text = thread.highlightText.orEmpty()

            threadUnreadBadgeText.isVisible = thread.unreadCount > 0
            threadUnreadBadgeText.text = when {
                thread.unreadCount > 99 -> "99+"
                thread.unreadCount > 0 -> thread.unreadCount.toString()
                else -> ""
            }

            val hasPhoto = !thread.photoUrl.isNullOrBlank()
            threadAvatarImage.isVisible = hasPhoto
            threadGroupIcon.isVisible = thread.isGroup && !hasPhoto
            threadInitialsText.isVisible = !hasPhoto && !thread.isGroup
            threadInitialsText.text = thread.initials

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = threadAvatarImage,
                    photoUrl = thread.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                threadAvatarImage.setImageDrawable(null)
            }

            threadAvatarContainer.background = ContextCompat.getDrawable(
                context,
                if (thread.isGroup) {
                    R.drawable.bg_circle_primary_soft
                } else {
                    R.drawable.bg_circle_tertiary_soft
                }
            )

            threadInitialsText.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (thread.isGroup) {
                        R.color.fitto_green
                    } else {
                        R.color.fitto_text_dark
                    }
                )
            )

            root.setOnClickListener {
                onThreadClick(thread)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MessageThreadUiModel>() {
        override fun areItemsTheSame(
            oldItem: MessageThreadUiModel,
            newItem: MessageThreadUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: MessageThreadUiModel,
            newItem: MessageThreadUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
