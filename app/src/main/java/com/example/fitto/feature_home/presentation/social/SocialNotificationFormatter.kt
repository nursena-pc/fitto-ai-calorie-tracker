package com.example.fitto.feature_home.presentation.social

import android.content.Context
import com.example.fitto.R
import com.google.firebase.firestore.DocumentSnapshot

object SocialNotificationFormatter {

    fun fromDocument(
        context: Context,
        document: DocumentSnapshot
    ): FittoNotification? {
        val type = document.getString("type").orEmpty()
        val actorName = document.getString("actorName").orEmpty().ifBlank {
            context.getString(R.string.social_notification_someone)
        }
        val body = document.getString("body").orEmpty()
        val postId = document.getString("postId")
        val commentId = document.getString("commentId")
        val threadId = document.getString("threadId")
        val actorId = document.getString("actorId")
        val createdAt = document.getLong("createdAt") ?: 0L
        val readAt = document.getLong("readAt") ?: 0L

        val title = when (type) {
            FittoNotification.TYPE_POST_LIKE -> context.getString(R.string.social_notification_post_like, actorName)
            FittoNotification.TYPE_POST_COMMENT -> context.getString(R.string.social_notification_post_comment, actorName)
            FittoNotification.TYPE_FOLLOWING_POST -> context.getString(R.string.social_notification_following_post, actorName)
            FittoNotification.TYPE_COMMENT_REPLY -> context.getString(R.string.social_notification_comment_reply, actorName)
            FittoNotification.TYPE_COMMENT_LIKE -> context.getString(R.string.social_notification_comment_like, actorName)
            FittoNotification.TYPE_FOLLOW -> context.getString(R.string.social_notification_follow, actorName)
            FittoNotification.TYPE_UNFOLLOW -> context.getString(R.string.social_notification_unfollow, actorName)
            FittoNotification.TYPE_MESSAGE -> context.getString(R.string.social_notification_message, actorName)
            FittoNotification.TYPE_GROUP_MESSAGE -> context.getString(R.string.social_notification_group_message, actorName)
            FittoNotification.TYPE_GROUP_ADDED -> context.getString(R.string.social_notification_group_added, actorName)
            else -> document.getString("title").orEmpty().ifBlank { context.getString(R.string.social_notifications) }
        }

        val detail = when {
            body.isNotBlank() -> body
            type == FittoNotification.TYPE_FOLLOW -> context.getString(R.string.social_notification_follow_detail)
            type == FittoNotification.TYPE_UNFOLLOW -> context.getString(R.string.social_notification_unfollow_detail)
            type == FittoNotification.TYPE_GROUP_ADDED -> context.getString(R.string.social_notification_group_added_detail)
            type == FittoNotification.TYPE_FOLLOWING_POST -> context.getString(R.string.social_notification_following_post_detail)
            type == FittoNotification.TYPE_MESSAGE || type == FittoNotification.TYPE_GROUP_MESSAGE -> context.getString(R.string.social_notification_message_detail)
            else -> context.getString(R.string.social_notification_open_detail)
        }

        return FittoNotification(
            id = document.id,
            title = title,
            description = detail,
            iconRes = iconFor(type),
            type = type.ifBlank { FittoNotification.TYPE_GENERAL },
            targetId = when (type) {
                FittoNotification.TYPE_MESSAGE,
                FittoNotification.TYPE_GROUP_MESSAGE,
                FittoNotification.TYPE_GROUP_ADDED -> threadId
                FittoNotification.TYPE_FOLLOWING_POST -> postId
                FittoNotification.TYPE_FOLLOW,
                FittoNotification.TYPE_UNFOLLOW -> actorId
                else -> postId
            },
            secondaryTargetId = commentId,
            isRead = readAt > 0L,
            createdAt = createdAt
        )
    }

    private fun iconFor(type: String): Int {
        return when (type) {
            FittoNotification.TYPE_POST_LIKE,
            FittoNotification.TYPE_COMMENT_LIKE,
            FittoNotification.TYPE_FOLLOWING_POST -> R.drawable.ic_notifications
            FittoNotification.TYPE_POST_COMMENT -> R.drawable.ic_comment
            FittoNotification.TYPE_COMMENT_REPLY -> R.drawable.ic_reply
            FittoNotification.TYPE_FOLLOW,
            FittoNotification.TYPE_UNFOLLOW -> R.drawable.ic_person_add
            FittoNotification.TYPE_MESSAGE -> R.drawable.ic_messages
            FittoNotification.TYPE_GROUP_MESSAGE,
            FittoNotification.TYPE_GROUP_ADDED -> R.drawable.ic_group
            else -> R.drawable.ic_notifications
        }
    }
}
