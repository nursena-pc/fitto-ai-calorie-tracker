package com.example.fitto.feature_home.presentation.social

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemSocialCommentBinding

class SocialCommentAdapter(
    private val onReplyClick: (SocialComment) -> Unit,
    private val onLikeClick: (SocialComment) -> Unit
) : RecyclerView.Adapter<SocialCommentAdapter.CommentViewHolder>() {

    private var allComments: List<SocialComment> = emptyList()
    private var rows: List<CommentRow> = emptyList()
    private val expandedParentIds = mutableSetOf<String>()

    fun submitComments(comments: List<SocialComment>) {
        allComments = comments
        rebuildRows()
    }

    private fun rebuildRows() {
        val rootComments = allComments.filter { it.parentCommentId == null }
        val repliesByParent = allComments
            .filter { it.parentCommentId != null }
            .groupBy { it.parentCommentId.orEmpty() }

        val newRows = mutableListOf<CommentRow>()

        rootComments.forEach { rootComment ->
            val replies = repliesByParent[rootComment.id].orEmpty()
            val isExpanded = expandedParentIds.contains(rootComment.id)

            newRows.add(
                CommentRow.Comment(
                    comment = rootComment,
                    isReply = false,
                    repliesCount = replies.size,
                    isExpanded = isExpanded
                )
            )

            if (isExpanded) {
                replies.forEach { reply ->
                    newRows.add(
                        CommentRow.Comment(
                            comment = reply,
                            isReply = true,
                            repliesCount = 0,
                            isExpanded = false
                        )
                    )
                }
            }
        }

        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemSocialCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CommentViewHolder(binding)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val row = rows[position] as CommentRow.Comment
        holder.bind(row)
    }

    inner class CommentViewHolder(
        private val binding: ItemSocialCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: CommentRow.Comment) = with(binding) {
            val context = root.context
            val comment = row.comment

            val displayName = comment.userName.ifBlank {
                context.getString(R.string.social_default_user)
            }

            commentUserNameText.text = displayName
            commentBodyText.text = comment.text
            commentTimeText.text = formatRelativeTime(comment.createdAt)

            val initial = displayName.firstOrNull()?.uppercase() ?: "F"
            commentAvatarInitialText.text = initial

            val hasAvatar = !comment.userPhotoUrl.isNullOrBlank()
            commentAvatarImage.isVisible = hasAvatar
            commentAvatarInitialText.isVisible = !hasAvatar

            if (hasAvatar) {
                ProfilePhotoLoader.load(
                    imageView = commentAvatarImage,
                    photoUrl = comment.userPhotoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                commentAvatarImage.setImageDrawable(null)
            }

            val leftPadding = if (row.isReply) {
                context.resources.getDimensionPixelSize(R.dimen.comment_reply_start_padding)
            } else {
                0
            }

            commentRoot.setPadding(
                leftPadding,
                commentRoot.paddingTop,
                commentRoot.paddingRight,
                commentRoot.paddingBottom
            )

            val avatarSize = context.resources.getDimensionPixelSize(
                if (row.isReply) {
                    R.dimen.comment_reply_avatar_size
                } else {
                    R.dimen.comment_root_avatar_size
                }
            )

            commentAvatarContainer.layoutParams =
                commentAvatarContainer.layoutParams.apply {
                    width = avatarSize
                    height = avatarSize
                }

            commentAvatarImage.layoutParams =
                commentAvatarImage.layoutParams.apply {
                    width = avatarSize
                    height = avatarSize
                }

            commentLikeButton.setImageResource(
                if (comment.likedByMe) {
                    R.drawable.ic_favorite
                } else {
                    R.drawable.ic_favorite_border
                }
            )

            commentLikeButton.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (comment.likedByMe) {
                        R.color.fitto_green
                    } else {
                        android.R.color.darker_gray
                    }
                )
            )

            commentLikeCountText.isVisible = comment.likesCount > 0
            commentLikeCountText.text = comment.likesCount.toString()

            replyCommentText.setOnClickListener {
                onReplyClick(comment)
            }

            commentLikeButton.setOnClickListener {
                onLikeClick(comment)
            }

            toggleRepliesText.isVisible = row.repliesCount > 0

            if (row.repliesCount > 0) {
                toggleRepliesText.text = if (row.isExpanded) {
                    context.getString(R.string.social_hide_replies)
                } else {
                    context.getString(R.string.social_show_replies, row.repliesCount)
                }

                toggleRepliesText.setOnClickListener {
                    if (expandedParentIds.contains(comment.id)) {
                        expandedParentIds.remove(comment.id)
                    } else {
                        expandedParentIds.add(comment.id)
                    }

                    rebuildRows()
                }
            } else {
                toggleRepliesText.setOnClickListener(null)
            }
        }

        private fun formatRelativeTime(createdAt: Long): String {
            if (createdAt <= 0L) {
                return binding.root.context.getString(R.string.social_just_now)
            }

            return DateUtils.getRelativeTimeSpanString(
                createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }
    }

    sealed class CommentRow {
        data class Comment(
            val comment: SocialComment,
            val isReply: Boolean,
            val repliesCount: Int,
            val isExpanded: Boolean
        ) : CommentRow()
    }
}