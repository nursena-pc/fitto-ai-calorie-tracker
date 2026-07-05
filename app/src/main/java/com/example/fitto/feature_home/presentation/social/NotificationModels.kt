package com.example.fitto.feature_home.presentation.social

import androidx.annotation.DrawableRes

data class FittoNotification(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val type: String = TYPE_GENERAL,
    val targetId: String? = null,
    val secondaryTargetId: String? = null,
    val isRead: Boolean = true,
    val createdAt: Long = 0L
) {
    companion object {
        const val TYPE_GENERAL = "general"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_POST_LIKE = "post_like"
        const val TYPE_POST_COMMENT = "post_comment"
        const val TYPE_FOLLOWING_POST = "following_post"
        const val TYPE_COMMENT_REPLY = "comment_reply"
        const val TYPE_COMMENT_LIKE = "comment_like"
        const val TYPE_FOLLOW = "follow"
        const val TYPE_UNFOLLOW = "unfollow"
        const val TYPE_MESSAGE = "message"
        const val TYPE_GROUP_MESSAGE = "group_message"
        const val TYPE_GROUP_ADDED = "group_added"
    }
}

sealed class NotificationListItem {
    abstract val itemId: String

    data class Section(
        val title: String,
        val unreadCount: Int
    ) : NotificationListItem() {
        override val itemId: String = "section_$title"
    }

    data class Row(
        val notification: FittoNotification
    ) : NotificationListItem() {
        override val itemId: String = notification.id
    }
}
