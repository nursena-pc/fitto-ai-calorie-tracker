package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData

enum class ProfileAchievementState {
    EARNED,
    ACTIVE,
    LOCKED
}

data class ProfileAchievementUiModel(
    val id: String,
    val title: String,
    val description: String,
    val statusText: String,
    val progressText: String,
    val progressPercent: Int,
    @DrawableRes val iconResId: Int,
    val state: ProfileAchievementState,
    val targetMenuId: ProfileMenuId? = null
)

object ProfileAchievementContent {

    fun itemsFor(
        context: Context,
        menuId: ProfileMenuId,
        healthData: ProfileHealthData
    ): List<ProfileAchievementUiModel> {
        return when (menuId) {
            ProfileMenuId.ACHIEVEMENTS -> overviewItems(context, healthData)
            ProfileMenuId.ACHIEVEMENT_BADGES -> badgeItems(context, healthData)
            ProfileMenuId.ACHIEVEMENT_STREAK -> streakItems(context, healthData)
            ProfileMenuId.ACHIEVEMENT_WATER -> waterItems(context, healthData)
            else -> emptyList()
        }
    }

    private fun overviewItems(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileAchievementUiModel> {
        val streakDays = streakDayCount(healthData)
        val weightProgress = healthData.weightGoalProgressPercent.coerceIn(0, 100)
        val hasWaterTarget = healthData.waterTargetLiter > 0.0

        return listOf(
            ProfileAchievementUiModel(
                id = "overview_badges",
                title = context.getString(R.string.profile_achievement_overview_badges_title),
                description = context.getString(R.string.profile_achievement_overview_badges_desc),
                statusText = statusForOverview(context, weightProgress, 100),
                progressText = context.getString(R.string.profile_achievement_progress_percent_format, weightProgress, 100),
                progressPercent = weightProgress,
                iconResId = R.drawable.ic_star_outline,
                state = stateForProgress(weightProgress, 100),
                targetMenuId = ProfileMenuId.ACHIEVEMENT_BADGES
            ),
            ProfileAchievementUiModel(
                id = "overview_streak",
                title = context.getString(R.string.profile_achievement_overview_streak_title),
                description = context.getString(R.string.profile_achievement_overview_streak_desc),
                statusText = statusForOverview(context, streakDays, 30),
                progressText = context.getString(R.string.profile_achievement_progress_day_count_format, streakDays, 30),
                progressPercent = percent(streakDays, 30),
                iconResId = R.drawable.ic_history,
                state = stateForProgress(streakDays, 30),
                targetMenuId = ProfileMenuId.ACHIEVEMENT_STREAK
            ),
            ProfileAchievementUiModel(
                id = "overview_water",
                title = context.getString(R.string.profile_achievement_overview_water_title),
                description = context.getString(R.string.profile_achievement_overview_water_desc),
                statusText = if (hasWaterTarget) {
                    context.getString(R.string.profile_achievement_status_active)
                } else {
                    context.getString(R.string.profile_achievement_status_locked)
                },
                progressText = if (hasWaterTarget) {
                    context.getString(R.string.profile_liter_value_format, healthData.waterTargetLiter)
                } else {
                    context.getString(R.string.profile_data_not_saved_yet)
                },
                progressPercent = if (hasWaterTarget) 35 else 0,
                iconResId = R.drawable.ic_water_bottle,
                state = if (hasWaterTarget) ProfileAchievementState.ACTIVE else ProfileAchievementState.LOCKED,
                targetMenuId = ProfileMenuId.ACHIEVEMENT_WATER
            )
        )
    }

    private fun badgeItems(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileAchievementUiModel> {
        val weightProgress = healthData.weightGoalProgressPercent.coerceIn(0, 100)
        val hasFirstProfileStep = healthData.hasMainData
        val hasDailyGoal = healthData.dailyTargetCalories > 0

        val items = mutableListOf<ProfileAchievementUiModel>()
        items += singleStepItem(
            context = context,
            id = "profile_first_step",
            title = context.getString(R.string.profile_achievement_goal_setup_title),
            description = context.getString(R.string.profile_achievement_goal_setup_desc),
            isDone = hasFirstProfileStep,
            activeWhenNotDone = true,
            iconResId = R.drawable.ic_check_circle
        )

        items += stagedPercentItems(
            context = context,
            idPrefix = "weight_progress",
            currentPercent = weightProgress,
            targets = listOf(25, 50, 75, 100),
            titleFactory = { target -> context.getString(R.string.profile_achievement_weight_stage_title, target) },
            descFactory = { target -> context.getString(R.string.profile_achievement_weight_stage_desc, target) },
            iconResId = R.drawable.ic_monitor_weight
        )

        items += singleStepItem(
            context = context,
            id = "daily_goal_ready",
            title = context.getString(R.string.profile_achievement_daily_goal_title),
            description = context.getString(R.string.profile_achievement_daily_goal_desc),
            isDone = hasDailyGoal,
            activeWhenNotDone = hasFirstProfileStep,
            iconResId = R.drawable.ic_bullseye_arrow
        )

        return items
    }

    private fun streakItems(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileAchievementUiModel> {
        val currentDays = streakDayCount(healthData)
        return stagedCountItems(
            context = context,
            idPrefix = "streak",
            current = currentDays,
            targets = listOf(3, 5, 7, 14, 30),
            titleFactory = { target -> context.getString(R.string.profile_achievement_streak_stage_title, target) },
            descFactory = { target -> context.getString(R.string.profile_achievement_streak_stage_desc, target) },
            iconResId = R.drawable.ic_history,
            progressFactory = { current, target -> context.getString(R.string.profile_achievement_progress_day_count_format, current, target) }
        )
    }

    private fun waterItems(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileAchievementUiModel> {
        val hasWaterTarget = healthData.waterTargetLiter > 0.0
        val setupValue = if (hasWaterTarget) 1 else 0
        val currentWaterDays = if (hasWaterTarget) 1 else 0

        val items = mutableListOf<ProfileAchievementUiModel>()
        items += ProfileAchievementUiModel(
            id = "water_setup",
            title = context.getString(R.string.profile_achievement_water_setup_title),
            description = context.getString(R.string.profile_achievement_water_setup_desc),
            statusText = if (hasWaterTarget) {
                context.getString(R.string.profile_achievement_status_earned)
            } else {
                context.getString(R.string.profile_achievement_status_active)
            },
            progressText = context.getString(R.string.profile_achievement_progress_count_format, setupValue, 1),
            progressPercent = percent(setupValue, 1),
            iconResId = R.drawable.ic_water_bottle,
            state = if (hasWaterTarget) ProfileAchievementState.EARNED else ProfileAchievementState.ACTIVE
        )

        items += stagedCountItems(
            context = context,
            idPrefix = "water_streak",
            current = currentWaterDays,
            targets = listOf(3, 5, 7, 14),
            titleFactory = { target -> context.getString(R.string.profile_achievement_water_stage_title, target) },
            descFactory = { target -> context.getString(R.string.profile_achievement_water_stage_desc, target) },
            iconResId = R.drawable.ic_water_bottle,
            progressFactory = { current, target -> context.getString(R.string.profile_achievement_progress_day_count_format, current, target) },
            forceLocked = !hasWaterTarget
        )

        return items
    }

    private fun stagedPercentItems(
        context: Context,
        idPrefix: String,
        currentPercent: Int,
        targets: List<Int>,
        titleFactory: (Int) -> String,
        descFactory: (Int) -> String,
        @DrawableRes iconResId: Int
    ): List<ProfileAchievementUiModel> {
        val nextTarget = targets.firstOrNull { currentPercent < it }
        return targets.map { target ->
            val state = when {
                currentPercent >= target -> ProfileAchievementState.EARNED
                target == nextTarget -> ProfileAchievementState.ACTIVE
                else -> ProfileAchievementState.LOCKED
            }
            ProfileAchievementUiModel(
                id = "${idPrefix}_$target",
                title = titleFactory(target),
                description = descFactory(target),
                statusText = statusForState(context, state),
                progressText = context.getString(
                    R.string.profile_achievement_progress_percent_format,
                    currentPercent.coerceAtMost(target),
                    target
                ),
                progressPercent = percent(currentPercent, target),
                iconResId = iconResId,
                state = state
            )
        }
    }

    private fun stagedCountItems(
        context: Context,
        idPrefix: String,
        current: Int,
        targets: List<Int>,
        titleFactory: (Int) -> String,
        descFactory: (Int) -> String,
        @DrawableRes iconResId: Int,
        progressFactory: (Int, Int) -> String,
        forceLocked: Boolean = false
    ): List<ProfileAchievementUiModel> {
        val nextTarget = targets.firstOrNull { current < it }
        return targets.map { target ->
            val state = when {
                forceLocked -> ProfileAchievementState.LOCKED
                current >= target -> ProfileAchievementState.EARNED
                target == nextTarget -> ProfileAchievementState.ACTIVE
                else -> ProfileAchievementState.LOCKED
            }
            ProfileAchievementUiModel(
                id = "${idPrefix}_$target",
                title = titleFactory(target),
                description = descFactory(target),
                statusText = statusForState(context, state),
                progressText = progressFactory(current.coerceAtMost(target), target),
                progressPercent = percent(current, target),
                iconResId = iconResId,
                state = state
            )
        }
    }

    private fun singleStepItem(
        context: Context,
        id: String,
        title: String,
        description: String,
        isDone: Boolean,
        activeWhenNotDone: Boolean,
        @DrawableRes iconResId: Int
    ): ProfileAchievementUiModel {
        val state = when {
            isDone -> ProfileAchievementState.EARNED
            activeWhenNotDone -> ProfileAchievementState.ACTIVE
            else -> ProfileAchievementState.LOCKED
        }
        return ProfileAchievementUiModel(
            id = id,
            title = title,
            description = description,
            statusText = statusForState(context, state),
            progressText = context.getString(R.string.profile_achievement_progress_count_format, if (isDone) 1 else 0, 1),
            progressPercent = if (isDone) 100 else 0,
            iconResId = iconResId,
            state = state
        )
    }

    private fun statusForOverview(context: Context, current: Int, target: Int): String {
        return statusForState(context, stateForProgress(current, target))
    }

    private fun stateForProgress(current: Int, target: Int): ProfileAchievementState {
        return when {
            current >= target -> ProfileAchievementState.EARNED
            current > 0 -> ProfileAchievementState.ACTIVE
            else -> ProfileAchievementState.LOCKED
        }
    }

    private fun statusForState(context: Context, state: ProfileAchievementState): String {
        return when (state) {
            ProfileAchievementState.EARNED -> context.getString(R.string.profile_achievement_status_earned)
            ProfileAchievementState.ACTIVE -> context.getString(R.string.profile_achievement_status_active)
            ProfileAchievementState.LOCKED -> context.getString(R.string.profile_achievement_status_locked)
        }
    }

    private fun percent(current: Int, target: Int): Int {
        if (target <= 0) return 0
        return (current.toFloat() / target.toFloat() * 100f).toInt().coerceIn(0, 100)
    }

    private fun streakDayCount(healthData: ProfileHealthData): Int {
        return healthData.history.size.coerceAtLeast(if (healthData.currentWeightKg > 0) 1 else 0)
    }
}
