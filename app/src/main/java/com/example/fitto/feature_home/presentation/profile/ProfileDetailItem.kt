package com.example.fitto.feature_home.presentation.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class ProfileDetailAction {
    REGION,
    LANGUAGE,
    THEME,
    ACCOUNT_VISIBILITY,
    EDIT_EMAIL,
    EDIT_USERNAME,
    EDIT_DISPLAY_NAME,
    CHANGE_PASSWORD,
    NOTIFICATION_SOUND,
    SILENT_HOURS,
    TOGGLE_COMMENTS,
    UNBLOCK_USER,
    TOGGLE_NOTIFICATIONS_ALLOWED,
    TOGGLE_ACHIEVEMENT_NOTIFICATIONS,
    TOGGLE_REMINDER_NOTIFICATIONS,
    TOGGLE_SOCIAL_NOTIFICATIONS,
    TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS,
    TOGGLE_GROUP_MESSAGE_NOTIFICATIONS,
    TOGGLE_VIBRATION,
    EXACT_ALARM_PERMISSION,
    LOGOUT,
    CLEAR_HISTORY,
    EXPORT_DATA,
    DELETE_ACCOUNT,
    RECALCULATE_GOALS,
    CONNECT_HEALTH,
    NUTRITION_DIET_TYPES,
    NUTRITION_ALLERGIES,
    NUTRITION_DISLIKED_FOODS,
    NUTRITION_MEAL_ROUTINE
}

data class ProfileDetailItem(
    @StringRes val titleResId: Int,
    val titleText: String? = null,
    @StringRes val subtitleResId: Int? = null,
    @DrawableRes val iconResId: Int? = null,
    val subtitleText: String? = null,
    val trailingText: String? = null,
    @StringRes val sectionTitleResId: Int? = null,
    val showDividerAfter: Boolean = false,
    val action: ProfileDetailAction? = null,
    val targetMenuId: ProfileMenuId? = null,
    val isDanger: Boolean = false,
    val isSwitch: Boolean = false,
    val switchChecked: Boolean = false,
    val isInfoCard: Boolean = false,
    val isPrimaryButton: Boolean = false,
    val showChevron: Boolean = true,
    val relatedUserId: String? = null,
    val isEnabled: Boolean = true
)
