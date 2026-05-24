package com.example.fitto.feature_home.presentation.profile

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.google.android.material.card.MaterialCardView

class ProfileAchievementAdapter(
    private val onAchievementClick: (ProfileAchievementUiModel) -> Unit
) : ListAdapter<ProfileAchievementUiModel, ProfileAchievementAdapter.AchievementViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_achievement_stage, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardProfileAchievement)
        private val iconContainer: View = itemView.findViewById(R.id.viewAchievementIconContainer)
        private val icon: ImageView = itemView.findViewById(R.id.imageAchievementIcon)
        private val statusText: TextView = itemView.findViewById(R.id.textAchievementStatus)
        private val titleText: TextView = itemView.findViewById(R.id.textAchievementTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.textAchievementDescription)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressAchievement)
        private val progressText: TextView = itemView.findViewById(R.id.textAchievementProgress)
        private val chevronText: TextView = itemView.findViewById(R.id.textAchievementChevron)

        fun bind(item: ProfileAchievementUiModel) {
            val context = itemView.context
            val green = ContextCompat.getColor(context, R.color.fitto_green)
            val orange = ContextCompat.getColor(context, R.color.fitto_orange)
            val locked = ContextCompat.getColor(context, R.color.profile_subtext)
            val surface = ContextCompat.getColor(context, R.color.profile_menu_background)
            val earnedSurface = ContextCompat.getColor(context, R.color.fitto_badge_background)
            val activeSurface = ContextCompat.getColor(context, R.color.fitto_surface_soft)
            val lockedSurface = ContextCompat.getColor(context, R.color.profile_progress_track)
            val white = ContextCompat.getColor(context, R.color.white)

            val accentColor = when (item.state) {
                ProfileAchievementState.EARNED -> green
                ProfileAchievementState.ACTIVE -> orange
                ProfileAchievementState.LOCKED -> locked
            }

            val cardColor = when (item.state) {
                ProfileAchievementState.EARNED -> earnedSurface
                ProfileAchievementState.ACTIVE -> activeSurface
                ProfileAchievementState.LOCKED -> surface
            }

            card.setCardBackgroundColor(cardColor)
            card.strokeColor = accentColor
            card.strokeWidth = if (item.state == ProfileAchievementState.ACTIVE) {
                context.resources.getDimensionPixelSize(R.dimen.profile_achievement_active_stroke)
            } else {
                context.resources.getDimensionPixelSize(R.dimen.profile_achievement_normal_stroke)
            }
            card.alpha = if (item.state == ProfileAchievementState.LOCKED) 0.62f else 1f

            icon.setImageResource(item.iconResId)
            icon.setColorFilter(accentColor)
            iconContainer.backgroundTintList = ColorStateList.valueOf(
                if (item.state == ProfileAchievementState.LOCKED) lockedSurface else white
            )

            statusText.text = item.statusText
            statusText.setTextColor(if (item.state == ProfileAchievementState.LOCKED) locked else accentColor)
            statusText.backgroundTintList = ColorStateList.valueOf(
                when (item.state) {
                    ProfileAchievementState.EARNED -> ContextCompat.getColor(context, R.color.fitto_primary_extra_soft)
                    ProfileAchievementState.ACTIVE -> ContextCompat.getColor(context, R.color.fitto_tertiary_soft)
                    ProfileAchievementState.LOCKED -> ContextCompat.getColor(context, R.color.profile_progress_track)
                }
            )

            titleText.text = item.title
            titleText.setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            descriptionText.text = item.description
            descriptionText.setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            progressText.text = item.progressText
            progressText.setTextColor(if (item.state == ProfileAchievementState.LOCKED) locked else accentColor)

            progressBar.progress = item.progressPercent
            progressBar.progressTintList = ColorStateList.valueOf(accentColor)
            progressBar.progressBackgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.profile_progress_track)
            )

            val canOpen = item.targetMenuId != null
            chevronText.isVisible = canOpen
            card.isClickable = canOpen
            card.isFocusable = canOpen
            card.setOnClickListener {
                if (canOpen) onAchievementClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProfileAchievementUiModel>() {
        override fun areItemsTheSame(
            oldItem: ProfileAchievementUiModel,
            newItem: ProfileAchievementUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProfileAchievementUiModel,
            newItem: ProfileAchievementUiModel
        ): Boolean = oldItem == newItem
    }
}
