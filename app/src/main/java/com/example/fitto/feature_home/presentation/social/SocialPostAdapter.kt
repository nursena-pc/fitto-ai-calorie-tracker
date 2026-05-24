package com.example.fitto.feature_home.presentation.social

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemSocialPostBinding
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import android.text.TextUtils
import com.google.android.material.chip.ChipGroup
import com.example.fitto.feature_recipes.util.RecipeImageLoader
class SocialPostAdapter(
    private val onLikeClick: (SocialPost) -> Unit,
    private val onCommentClick: (SocialPost) -> Unit,
    private val onSaveClick: (SocialPost) -> Unit,
    private val onShareClick: (SocialPost) -> Unit,
    private val onReportClick: (SocialPost) -> Unit,
    private val onUserClick: (SocialPost) -> Unit,
    private val onRecipeClick: (SocialPost) -> Unit = {}
) : ListAdapter<SocialPost, SocialPostAdapter.SocialPostViewHolder>(DiffCallback) {

    private val expandedPostIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialPostViewHolder {
        val binding = ItemSocialPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SocialPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SocialPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SocialPostViewHolder(
        private val binding: ItemSocialPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private fun dp(value: Int): Int {
            return (value * binding.root.resources.displayMetrics.density).toInt()
        }
        private fun createTagChip(
            tag: String,
            index: Int
        ): Chip {
            val context = binding.root.context

            val bgColorRes = when (index % 4) {
                0 -> R.color.tag_chip_green_bg
                1 -> R.color.tag_chip_blue_bg
                2 -> R.color.tag_chip_yellow_bg
                else -> R.color.tag_chip_purple_bg
            }

            val textColorRes = when (index % 4) {
                0 -> R.color.tag_chip_green_text
                1 -> R.color.tag_chip_blue_text
                2 -> R.color.tag_chip_yellow_text
                else -> R.color.tag_chip_purple_text
            }

            return Chip(context).apply {
                text = tag

                isCheckable = false
                isClickable = false
                isCloseIconVisible = false
                isCheckedIconVisible = false
                isFocusable = false

                // Yazı boyutu aynı kalsın
                textSize = 12f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false

                setEnsureMinTouchTargetSize(false)

                // Burada çok küçük yükseklik vermiyoruz, yoksa yazı kesiliyor
                chipMinHeight = dp(30).toFloat()
                chipCornerRadius = dp(14).toFloat()
                chipStrokeWidth = 0f

                // Kutunun iç boşlukları
                chipStartPadding = dp(10).toFloat()
                chipEndPadding = dp(10).toFloat()
                textStartPadding = 0f
                textEndPadding = 0f

                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, bgColorRes)
                )

                setTextColor(
                    ContextCompat.getColor(context, textColorRes)
                )

                layoutParams = ChipGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(8)
                    bottomMargin = dp(8)
                }
            }
        }

        fun bind(post: SocialPost) = with(binding) {
            val context = root.context

            val displayName = post.userName.ifBlank {
                context.getString(R.string.social_default_user)
            }

            userNameText.text = displayName
            timeText.text = formatRelativeTime(post.createdAt)

            val initial = displayName.firstOrNull()?.uppercase() ?: "F"
            avatarInitialText.text = initial

            val hasAvatar = !post.userPhotoUrl.isNullOrBlank()
            avatarImage.isVisible = hasAvatar
            avatarInitialText.isVisible = !hasAvatar

            if (hasAvatar) {
                ProfilePhotoLoader.load(
                    imageView = avatarImage,
                    photoUrl = post.userPhotoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                avatarImage.setImageDrawable(null)
            }

            avatarContainer.setOnClickListener { onUserClick(post) }
            userNameText.setOnClickListener { onUserClick(post) }

            bindContent(post)

            val hasPostImage = !post.imageUrl.isNullOrBlank()
            postImageContainer.isVisible = hasPostImage
            if (hasPostImage) {
                postImage.load(post.imageUrl)
            } else {
                postImage.setImageDrawable(null)
            }

            val calorieText = post.calories?.let {
                context.getString(R.string.social_kcal_value, it)
            }
            postTagsChipGroup.removeAllViews()
            postTagsChipGroup.isVisible = post.tags.isNotEmpty()

            post.tags
                .take(6)
                .forEachIndexed { index, tag ->
                    postTagsChipGroup.addView(
                        createTagChip(
                            tag = tag,
                            index = index
                        )
                    )
                }

            imageCalorieBadgeText.isVisible = hasPostImage && calorieText != null
            imageCalorieBadgeText.text = calorieText

            standaloneCalorieBadgeText.isVisible = !hasPostImage && calorieText != null
            standaloneCalorieBadgeText.text = calorieText

            mealInfoText.isVisible = !post.mealInfo.isNullOrBlank()
            mealInfoText.text = post.mealInfo.orEmpty()

            bindRecipePreview(post)

            val showActions = !post.isDraft
            val canReportPost = showActions && !post.isMine

            likeButton.isVisible = showActions
            commentButton.isVisible = showActions && post.allowComments
            shareButton.isVisible = showActions
            saveButton.isVisible = showActions
            moreButtonContainer.isVisible = canReportPost
            moreButton.isVisible = canReportPost

            likeButton.text = post.likesCount.toString()
            likeButton.setIconResource(
                if (post.likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )

            val likedColor = if (post.likedByMe) {
                ContextCompat.getColor(context, R.color.fitto_green)
            } else {
                ContextCompat.getColor(context, R.color.fitto_text_secondary)
            }

            likeButton.iconTint = ColorStateList.valueOf(likedColor)
            likeButton.setTextColor(likedColor)

            commentButton.isVisible = showActions && post.allowComments
            commentButton.text = post.commentsCount.toString()
            commentButton.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.fitto_text_secondary)
            )
            commentButton.setTextColor(
                ContextCompat.getColor(context, R.color.fitto_text_secondary)
            )

            shareButton.text = ""
            shareButton.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.fitto_text_secondary)
            )

            saveButton.text = ""
            saveButton.setIconResource(
                if (post.savedByMe) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
            )

            val savedColor = if (post.savedByMe) {
                ContextCompat.getColor(context, R.color.fitto_green)
            } else {
                ContextCompat.getColor(context, R.color.fitto_text_secondary)
            }

            saveButton.iconTint = ColorStateList.valueOf(savedColor)
            saveButton.setTextColor(savedColor)

            likeButton.setOnClickListener { onLikeClick(post) }
            commentButton.setOnClickListener { onCommentClick(post) }
            saveButton.setOnClickListener { onSaveClick(post) }
            shareButton.setOnClickListener { onShareClick(post) }

            val moreClickListener = View.OnClickListener {
                onReportClick(post)
            }
            moreButtonContainer.setOnClickListener(moreClickListener)
            moreButton.setOnClickListener(moreClickListener)
        }


        private fun ItemSocialPostBinding.bindRecipePreview(post: SocialPost) {
            val context = root.context
            val hasRecipe = post.recipeId?.isNotBlank() == true || post.postType == "recipe"

            recipePreviewCard.isVisible = hasRecipe
            if (!hasRecipe) {
                recipePreviewCard.setOnClickListener(null)
                return
            }

            recipePreviewTitleText.text = post.recipeTitle.orEmpty().ifBlank {
                post.mealInfo.orEmpty().ifBlank { context.getString(R.string.recipe_post_card_title_fallback) }
            }

            val meta = listOfNotNull(
                post.recipeDuration?.takeIf { it.isNotBlank() },
                post.recipeServing?.takeIf { it.isNotBlank() },
                post.recipeCalories?.let { context.getString(R.string.social_kcal_value, it) }
            ).joinToString(" • ")
            recipePreviewMetaText.text = meta
            recipePreviewMetaText.isVisible = meta.isNotBlank()

            val category = post.recipeCategory.orEmpty().ifBlank { context.getString(R.string.recipe_post_card_label) }
            recipePreviewBadgeText.text = category.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }

            RecipeImageLoader.load(
                imageView = recipePreviewImage,
                recipeId = post.recipeId,
                fallbackResId = post.recipeImageResId?.takeIf { it != 0 }
                    ?: R.drawable.profile_avatar_04
            )

            recipePreviewCard.setOnClickListener { onRecipeClick(post) }
        }

        private fun ItemSocialPostBinding.bindContent(post: SocialPost) {
            val hasContent = post.content.isNotBlank()
            val isExpanded = expandedPostIds.contains(post.id)

            contentText.isVisible = hasContent
            contentMoreText.isVisible = false

            if (!hasContent) {
                contentText.text = ""
                contentText.setOnClickListener(null)
                contentMoreText.setOnClickListener(null)
                return
            }

            contentText.text = post.content
            contentText.maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES

            val toggleExpansion = {
                if (expandedPostIds.contains(post.id)) {
                    expandedPostIds.remove(post.id)
                } else {
                    expandedPostIds.add(post.id)
                }

                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position)
                }
            }

            contentText.post {
                if (root.parent == null) return@post

                val layout = contentText.layout
                val lineCount = layout?.lineCount ?: 0
                val lastLineIndex = (lineCount - 1).coerceAtLeast(0)
                val isEllipsized = layout?.getEllipsisCount(lastLineIndex)?.let { it > 0 } == true
                val isLongEnough = post.content.length > COLLAPSED_TEXT_LENGTH || post.content.count { it == '\n' } >= COLLAPSED_MAX_LINES

                val shouldShowControl = isExpanded || isEllipsized || isLongEnough
                contentMoreText.isVisible = shouldShowControl
                contentMoreText.text = if (isExpanded) {
                    root.context.getString(R.string.social_show_less)
                } else {
                    root.context.getString(R.string.social_show_more)
                }
            }

            contentMoreText.setOnClickListener { toggleExpansion() }
            contentText.setOnClickListener {
                if (contentMoreText.isVisible) {
                    toggleExpansion()
                }
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

    private object DiffCallback : DiffUtil.ItemCallback<SocialPost>() {
        override fun areItemsTheSame(oldItem: SocialPost, newItem: SocialPost): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SocialPost, newItem: SocialPost): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val COLLAPSED_MAX_LINES = 5
        private const val COLLAPSED_TEXT_LENGTH = 120
    }
}
