package com.example.fitto.feature_home.presentation.social

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemNotificationBinding
import com.example.fitto.databinding.ItemNotificationSectionHeaderBinding

class NotificationAdapter(
    private val onNotificationClick: (FittoNotification) -> Unit
) : ListAdapter<NotificationListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.Section -> VIEW_TYPE_SECTION
            is NotificationListItem.Row -> VIEW_TYPE_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SECTION) {
            SectionViewHolder(
                ItemNotificationSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            NotificationViewHolder(
                ItemNotificationBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationListItem.Section -> (holder as SectionViewHolder).bind(item)
            is NotificationListItem.Row -> (holder as NotificationViewHolder).bind(item.notification)
        }
    }

    inner class SectionViewHolder(
        private val binding: ItemNotificationSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: NotificationListItem.Section) = with(binding) {
            notificationSectionTitleText.text = section.title
            notificationSectionBadgeText.isVisible = section.unreadCount > 0
            notificationSectionBadgeText.text = if (section.unreadCount > 99) "99+" else section.unreadCount.toString()
        }
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: FittoNotification) = with(binding) {
            val context = root.context
            notificationTitleText.text = notification.title
            notificationDescriptionText.text = notification.description
            notificationIcon.setImageResource(notification.iconRes)
            notificationUnreadDot.isVisible = !notification.isRead

            notificationCard.alpha = if (notification.isRead) 0.58f else 1f
            notificationCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (notification.isRead) R.color.fitto_surface else R.color.fitto_surface_soft
                )
            )
            notificationCard.strokeWidth = if (notification.isRead) 0 else 2
            notificationCard.strokeColor = ContextCompat.getColor(context, R.color.fitto_primary_soft)

            notificationTitleText.setTypeface(null, if (notification.isRead) Typeface.NORMAL else Typeface.BOLD)
            notificationDescriptionText.setTypeface(null, if (notification.isRead) Typeface.NORMAL else Typeface.BOLD)

            root.setOnClickListener { onNotificationClick(notification) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<NotificationListItem>() {
        override fun areItemsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_SECTION = 1
        private const val VIEW_TYPE_ROW = 2
    }
}
