package com.example.fitto.feature_home.presentation.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.fitto.R

enum class ProfileMenuId {
    NOTIFICATIONS,
    SETTINGS,
    WEIGHT,
    BODY_MEASUREMENTS,
    ACHIEVEMENTS,
    REMINDERS,
    PHOTO_ALBUM,
    PRIVACY,
    CONTACT,
    ACCOUNT_MANAGEMENT,
    DAILY_GOALS,
    NUTRITION_PREFERENCES,
    NOTIFICATION_PREFERENCES,
    BLOCKED_USERS,
    CONNECTED_APPS,
    HELP_FEEDBACK,
    PRIVACY_POLICY,
    TERMS,
    ACHIEVEMENT_BADGES,
    ACHIEVEMENT_STREAK,
    ACHIEVEMENT_WATER
}

data class ProfileMenuItem(
    val id: ProfileMenuId,
    @StringRes val titleResId: Int,
    @DrawableRes val iconResId: Int
) {
    companion object {
        fun defaultItems(): List<ProfileMenuItem> {
            return listOf(
                ProfileMenuItem(
                    id = ProfileMenuId.WEIGHT,
                    titleResId = R.string.profile_menu_weight,
                    iconResId = R.drawable.ic_monitor_weight
                ),
                ProfileMenuItem(
                    id = ProfileMenuId.BODY_MEASUREMENTS,
                    titleResId = R.string.profile_menu_body_measurements,
                    iconResId = R.drawable.ic_straighten
                ),
                ProfileMenuItem(
                    id = ProfileMenuId.ACHIEVEMENTS,
                    titleResId = R.string.profile_menu_achievements,
                    iconResId = R.drawable.ic_star_outline
                ),
                ProfileMenuItem(
                    id = ProfileMenuId.REMINDERS,
                    titleResId = R.string.profile_menu_reminders,
                    iconResId = R.drawable.ic_history
                ),
                ProfileMenuItem(
                    id = ProfileMenuId.PHOTO_ALBUM,
                    titleResId = R.string.profile_menu_photo_album,
                    iconResId = R.drawable.ic_image
                ),
                ProfileMenuItem(
                    id = ProfileMenuId.SETTINGS,
                    titleResId = R.string.profile_menu_settings,
                    iconResId = R.drawable.ic_cog
                ),
                ProfileMenuItem(
                    id = ProfileMenuId.CONTACT,
                    titleResId = R.string.profile_menu_contact,
                    iconResId = R.drawable.ic_email
                )
            )
        }
    }
}
