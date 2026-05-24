package com.example.fitto.feature_home.presentation.profile

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg
import com.example.fitto.feature_home.presentation.social.SocialUser

private const val DEFAULT_WATER_TARGET_LITER = 2.0

object ProfileDetailContent {

    @StringRes
    fun titleFor(menuId: ProfileMenuId): Int {
        return when (menuId) {
            ProfileMenuId.NOTIFICATIONS -> R.string.profile_menu_notifications
            ProfileMenuId.SETTINGS -> R.string.profile_menu_settings
            ProfileMenuId.WEIGHT -> R.string.profile_menu_weight
            ProfileMenuId.BODY_MEASUREMENTS -> R.string.profile_menu_body_measurements
            ProfileMenuId.ACHIEVEMENTS -> R.string.profile_menu_achievements
            ProfileMenuId.REMINDERS -> R.string.profile_menu_reminders
            ProfileMenuId.PHOTO_ALBUM -> R.string.profile_menu_photo_album
            ProfileMenuId.PRIVACY -> R.string.profile_menu_privacy
            ProfileMenuId.CONTACT -> R.string.profile_menu_contact
            ProfileMenuId.ACCOUNT_MANAGEMENT -> R.string.profile_detail_settings_account
            ProfileMenuId.DAILY_GOALS -> R.string.profile_detail_settings_daily_goals
            ProfileMenuId.NUTRITION_PREFERENCES -> R.string.nutrition_preferences_title
            ProfileMenuId.NOTIFICATION_PREFERENCES -> R.string.profile_detail_notification_preferences
            ProfileMenuId.BLOCKED_USERS -> R.string.profile_detail_blocked_users
            ProfileMenuId.CONNECTED_APPS -> R.string.profile_detail_connected_apps
            ProfileMenuId.HELP_FEEDBACK -> R.string.profile_detail_help_feedback
            ProfileMenuId.PRIVACY_POLICY -> R.string.profile_detail_privacy_policy
            ProfileMenuId.TERMS -> R.string.profile_detail_terms
            ProfileMenuId.ACHIEVEMENT_BADGES -> R.string.profile_achievement_badges_title
            ProfileMenuId.ACHIEVEMENT_STREAK -> R.string.profile_achievement_streak_title
            ProfileMenuId.ACHIEVEMENT_WATER -> R.string.profile_achievement_water_title
        }
    }

    fun rowsFor(
        context: Context,
        menuId: ProfileMenuId,
        healthData: ProfileHealthData = ProfileHealthData.Empty,
        user: SocialUser? = null,
        blockedUsers: List<SocialUser> = emptyList()
    ): List<ProfileDetailItem> {
        return when (menuId) {
            ProfileMenuId.SETTINGS -> settingsRows(context)
            ProfileMenuId.WEIGHT -> weightRows(context = context, healthData = healthData)
            ProfileMenuId.BODY_MEASUREMENTS -> bodyMeasurementInfoRows()
            ProfileMenuId.ACHIEVEMENTS -> achievementsRows()
            ProfileMenuId.REMINDERS -> reminderRows()
            ProfileMenuId.PHOTO_ALBUM -> albumRows()
            ProfileMenuId.CONTACT -> contactRows()
            ProfileMenuId.PRIVACY -> privacyRows(context)
            ProfileMenuId.NOTIFICATIONS -> notificationListInfoRows()
            ProfileMenuId.ACCOUNT_MANAGEMENT -> accountManagementRows(context, user)
            ProfileMenuId.DAILY_GOALS -> dailyGoalsRows(context = context, healthData = healthData)
            ProfileMenuId.NUTRITION_PREFERENCES -> nutritionPreferenceRows(context)
            ProfileMenuId.NOTIFICATION_PREFERENCES -> notificationPreferenceRows(context)
            ProfileMenuId.BLOCKED_USERS -> blockedUsersRows(blockedUsers)
            ProfileMenuId.CONNECTED_APPS -> connectedAppsRows(context)
            ProfileMenuId.HELP_FEEDBACK -> helpFeedbackRows()
            ProfileMenuId.PRIVACY_POLICY -> privacyPolicyRows()
            ProfileMenuId.TERMS -> termsRows()
            ProfileMenuId.ACHIEVEMENT_BADGES -> achievementBadgesRows(context, healthData)
            ProfileMenuId.ACHIEVEMENT_STREAK -> achievementStreakRows(context, healthData)
            ProfileMenuId.ACHIEVEMENT_WATER -> achievementWaterRows(context, healthData)
        }
    }

    private fun settingsRows(context: Context): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_account,
                titleResId = R.string.profile_detail_settings_account,
                subtitleResId = R.string.profile_detail_settings_account_desc,
                iconResId = R.drawable.ic_account_cog,
                targetMenuId = ProfileMenuId.ACCOUNT_MANAGEMENT
            ),

            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_preferences,
                titleResId = R.string.profile_detail_settings_region,
                trailingText = ProfilePreferenceStore.getRegion(context),
                iconResId = R.drawable.ic_public,
                action = ProfileDetailAction.REGION
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_settings_language,
                trailingText = ProfilePreferenceStore.languageText(context),
                iconResId = R.drawable.ic_translate,
                action = ProfileDetailAction.LANGUAGE
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_settings_theme,
                trailingText = ProfilePreferenceStore.themeText(context),
                iconResId = R.drawable.ic_theme_sun,
                action = ProfileDetailAction.THEME,
                showDividerAfter = true
            ),

            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_goals_nutrition,
                titleResId = R.string.profile_detail_settings_daily_goals,
                subtitleResId = R.string.profile_detail_settings_daily_goals_desc,
                iconResId = R.drawable.ic_bullseye_arrow,
                targetMenuId = ProfileMenuId.DAILY_GOALS
            ),
            ProfileDetailItem(
                titleResId = R.string.nutrition_preferences_title,
                subtitleResId = R.string.nutrition_preferences_desc,
                iconResId = R.drawable.ic_nutrition_fork,
                targetMenuId = ProfileMenuId.NUTRITION_PREFERENCES,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_notifications,
                titleResId = R.string.profile_detail_notification_preferences,
                subtitleResId = R.string.profile_detail_notification_preferences_desc,
                iconResId = R.drawable.ic_notifications,
                targetMenuId = ProfileMenuId.NOTIFICATION_PREFERENCES,
                showDividerAfter = true
            ),

            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_community,
                titleResId = R.string.profile_detail_privacy_visibility,
                trailingText = ProfilePreferenceStore.accountVisibilityText(context),
                iconResId = R.drawable.ic_visibility,
                action = ProfileDetailAction.ACCOUNT_VISIBILITY
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_privacy_comments,
                trailingText = ProfilePreferenceStore.onOffText(
                    context = context,
                    enabled = ProfilePreferenceStore.areCommentsEnabled(context)
                ),
                iconResId = R.drawable.ic_comment_multiple_outline,
                action = ProfileDetailAction.TOGGLE_COMMENTS,
                isSwitch = true,
                switchChecked = ProfilePreferenceStore.areCommentsEnabled(context)
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_blocked_users,
                subtitleResId = R.string.profile_detail_blocked_users_desc,
                iconResId = R.drawable.ic_block_user,
                targetMenuId = ProfileMenuId.BLOCKED_USERS,
                showDividerAfter = true
            ),

            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_connections,
                titleResId = R.string.profile_detail_connected_apps,
                subtitleText = ProfilePreferenceStore.connectedAppsSummary(context),
                iconResId = R.drawable.ic_link_variant,
                targetMenuId = ProfileMenuId.CONNECTED_APPS,
                showDividerAfter = true
            ),

            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_support,
                titleResId = R.string.profile_detail_help_feedback,
                subtitleResId = R.string.profile_detail_help_feedback_desc,
                iconResId = R.drawable.ic_info,
                targetMenuId = ProfileMenuId.HELP_FEEDBACK
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_privacy_policy,
                subtitleResId = R.string.profile_detail_privacy_policy_desc,
                iconResId = R.drawable.ic_shield,
                targetMenuId = ProfileMenuId.PRIVACY_POLICY
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_terms,
                subtitleResId = R.string.profile_detail_terms_desc,
                iconResId = R.drawable.ic_assignment_turned_in,
                targetMenuId = ProfileMenuId.TERMS,
                showDividerAfter = true
            ),

            ProfileDetailItem(
                titleResId = R.string.profile_detail_settings_logout,
                subtitleResId = R.string.profile_detail_settings_logout_desc,
                iconResId = R.drawable.ic_logout,
                action = ProfileDetailAction.LOGOUT,
                isDanger = true,
                showChevron = false
            )
        )
    }

    private fun accountManagementRows(
        context: Context,
        user: SocialUser?
    ): List<ProfileDetailItem> {
        val emailText = user?.email?.takeIf { it.isNotBlank() }
            ?: ProfilePreferenceStore.accountEmailText(context)
        val userNameText = user?.userName?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.profile_data_not_saved_yet)
        val displayNameText = user?.personalName?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.profile_data_not_saved_yet)

        return listOf(
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_account_info,
                titleResId = R.string.profile_detail_account_email,
                trailingText = emailText,
                action = ProfileDetailAction.EDIT_EMAIL
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_account_username,
                trailingText = userNameText,
                action = ProfileDetailAction.EDIT_USERNAME
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_account_display_name,
                trailingText = displayNameText,
                action = ProfileDetailAction.EDIT_DISPLAY_NAME
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_account_change_password,
                subtitleResId = R.string.profile_detail_account_change_password_desc,
                action = ProfileDetailAction.CHANGE_PASSWORD,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_my_data,
                titleResId = R.string.profile_detail_export_data,
                subtitleResId = R.string.profile_detail_export_data_desc,
                action = ProfileDetailAction.EXPORT_DATA,
                isPrimaryButton = true,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_clear_history,
                subtitleResId = R.string.profile_detail_clear_history_desc,
                action = ProfileDetailAction.CLEAR_HISTORY,
                isDanger = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_delete_account,
                subtitleResId = R.string.profile_detail_delete_account_desc,
                action = ProfileDetailAction.DELETE_ACCOUNT,
                isDanger = true,
                showChevron = false
            )
        )
    }

    private fun bodyMeasurementInfoRows(): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_body_measurements_title,
                subtitleResId = R.string.profile_body_measurements_entry_desc,
                showChevron = false
            )
        )
    }

    private fun dailyGoalsRows(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileDetailItem> {
        val calorieText = if (healthData.dailyTargetCalories > 0) {
            context.getString(R.string.profile_calorie_value_format, healthData.dailyTargetCalories)
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }

        val giText = if (healthData.maintenanceCalories > 0) {
            context.getString(R.string.profile_calorie_value_format, healthData.maintenanceCalories)
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }

        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_daily_goals_calorie,
                trailingText = calorieText,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_daily_goals_water,
                trailingText = context.getString(
                    R.string.profile_liter_value_format,
                    healthData.waterTargetLiter.takeIf { it > 0.0 } ?: DEFAULT_WATER_TARGET_LITER
                ),
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_daily_goals_protein,
                trailingText = gramText(context, healthData.proteinGoalGrams),
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_daily_goals_carbs,
                trailingText = gramText(context, healthData.carbGoalGrams),
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_daily_goals_fat,
                trailingText = gramText(context, healthData.fatGoalGrams),
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_daily_goals_gi_summary,
                titleResId = R.string.profile_daily_goals_gi_title,
                subtitleResId = R.string.profile_daily_goals_gi_desc,
                trailingText = giText,
                isInfoCard = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_daily_goals_recalculate,
                subtitleResId = R.string.profile_daily_goals_recalculate_desc,
                action = ProfileDetailAction.RECALCULATE_GOALS,
                isPrimaryButton = true,
                showChevron = false
            )
        )
    }


    private fun nutritionPreferenceRows(context: Context): List<ProfileDetailItem> {
        val allergies = ProfilePreferenceStore.getNutritionAllergies(context)
        val dislikedFoods = ProfilePreferenceStore.getNutritionDislikedFoods(context)

        return listOf(
            ProfileDetailItem(
                titleResId = R.string.nutrition_preferences_intro,
                subtitleText = ProfilePreferenceStore.nutritionPreferencesSummary(context),
                iconResId = R.drawable.ic_nutrition_fork,
                isInfoCard = true,
                showChevron = false,
                isEnabled = false
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.nutrition_pref_diet_type_title,
                titleResId = R.string.nutrition_pref_diet_type_title,
                subtitleResId = R.string.nutrition_pref_diet_type_desc,
                trailingText = ProfilePreferenceStore.nutritionDietPreferencesText(context),
                iconResId = R.drawable.ic_nutrition_fork,
                action = ProfileDetailAction.NUTRITION_DIET_TYPES
            ),
            ProfileDetailItem(
                titleResId = R.string.nutrition_pref_allergies_title,
                subtitleResId = R.string.nutrition_pref_allergies_desc,
                trailingText = allergies.ifBlank { context.getString(R.string.profile_data_not_saved_yet) },
                iconResId = R.drawable.ic_warning,
                action = ProfileDetailAction.NUTRITION_ALLERGIES
            ),
            ProfileDetailItem(
                titleResId = R.string.nutrition_pref_disliked_title,
                subtitleResId = R.string.nutrition_pref_disliked_desc,
                trailingText = dislikedFoods.ifBlank { context.getString(R.string.profile_data_not_saved_yet) },
                iconResId = R.drawable.ic_block_user,
                action = ProfileDetailAction.NUTRITION_DISLIKED_FOODS
            ),
            ProfileDetailItem(
                titleResId = R.string.nutrition_pref_meal_routine_title,
                subtitleResId = R.string.nutrition_pref_meal_routine_desc,
                trailingText = ProfilePreferenceStore.nutritionMealRoutineText(context),
                iconResId = R.drawable.ic_history,
                action = ProfileDetailAction.NUTRITION_MEAL_ROUTINE,
                showDividerAfter = true
            )
        )
    }

    private fun notificationPreferenceRows(context: Context): List<ProfileDetailItem> {
        val notificationsAllowed = ProfilePreferenceStore.areNotificationsAllowed(context)
        val remindersEnabled = ProfilePreferenceStore.areReminderNotificationsEnabled(context)
        val exactAlarmEnabled = hasExactAlarmPermission(context)
        val exactAlarmRelevant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        return listOf(
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_notification_general,
                titleResId = R.string.profile_notifications_allow,
                trailingText = ProfilePreferenceStore.onOffText(
                    context,
                    notificationsAllowed
                ),
                iconResId = R.drawable.ic_notifications,
                action = ProfileDetailAction.TOGGLE_NOTIFICATIONS_ALLOWED,
                isSwitch = true,
                switchChecked = notificationsAllowed,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_notification_types,
                titleResId = R.string.profile_detail_notifications_achievements,
                subtitleResId = R.string.profile_detail_notifications_achievements_desc,
                iconResId = R.drawable.ic_star_outline,
                action = ProfileDetailAction.TOGGLE_ACHIEVEMENT_NOTIFICATIONS,
                isSwitch = true,
                switchChecked = notificationsAllowed && ProfilePreferenceStore.areAchievementNotificationsEnabled(context),
                isEnabled = notificationsAllowed
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_notifications_reminders,
                subtitleResId = R.string.profile_detail_notifications_reminders_desc,
                iconResId = R.drawable.ic_alarm,
                action = ProfileDetailAction.TOGGLE_REMINDER_NOTIFICATIONS,
                isSwitch = true,
                switchChecked = notificationsAllowed && remindersEnabled,
                isEnabled = notificationsAllowed
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_notifications_social,
                subtitleResId = R.string.profile_detail_notifications_social_desc,
                iconResId = R.drawable.ic_group,
                action = ProfileDetailAction.TOGGLE_SOCIAL_NOTIFICATIONS,
                isSwitch = true,
                switchChecked = notificationsAllowed && ProfilePreferenceStore.areSocialNotificationsEnabled(context),
                isEnabled = notificationsAllowed
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_notifications_direct_messages,
                subtitleResId = R.string.profile_detail_notifications_direct_messages_desc,
                iconResId = R.drawable.ic_messages,
                action = ProfileDetailAction.TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS,
                isSwitch = true,
                switchChecked = notificationsAllowed && ProfilePreferenceStore.areDirectMessageNotificationsEnabled(context),
                isEnabled = notificationsAllowed
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_notifications_group_messages,
                subtitleResId = R.string.profile_detail_notifications_group_messages_desc,
                iconResId = R.drawable.ic_group,
                action = ProfileDetailAction.TOGGLE_GROUP_MESSAGE_NOTIFICATIONS,
                isSwitch = true,
                switchChecked = notificationsAllowed && ProfilePreferenceStore.areGroupMessageNotificationsEnabled(context),
                isEnabled = notificationsAllowed,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_sound_vibration,
                titleResId = R.string.profile_notifications_sound,
                trailingText = ProfilePreferenceStore.notificationSoundText(context),
                iconResId = R.drawable.ic_notify_sound,
                action = ProfileDetailAction.NOTIFICATION_SOUND,
                isEnabled = notificationsAllowed
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_notifications_vibration,
                trailingText = ProfilePreferenceStore.onOffText(
                    context,
                    ProfilePreferenceStore.isVibrationEnabled(context)
                ),
                iconResId = R.drawable.ic_notify_vibration,
                action = ProfileDetailAction.TOGGLE_VIBRATION,
                isSwitch = true,
                switchChecked = notificationsAllowed && ProfilePreferenceStore.isVibrationEnabled(context),
                isEnabled = notificationsAllowed,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_exact_alarm,
                titleResId = R.string.profile_notifications_exact_alarm,
                subtitleResId = R.string.profile_notifications_exact_alarm_desc,
                trailingText = exactAlarmStatusText(context, exactAlarmEnabled),
                iconResId = R.drawable.ic_alarm,
                action = ProfileDetailAction.EXACT_ALARM_PERMISSION,
                isEnabled = notificationsAllowed && exactAlarmRelevant,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_silent_hours,
                titleResId = R.string.profile_notifications_silent_hours,
                trailingText = ProfilePreferenceStore.silentHoursText(context),
                iconResId = R.drawable.ic_notify_silent,
                action = ProfileDetailAction.SILENT_HOURS,
                isEnabled = notificationsAllowed
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_notifications_silent_hours_note_title,
                subtitleResId = R.string.profile_notifications_silent_hours_note,
                iconResId = R.drawable.ic_info,
                isInfoCard = true,
                showChevron = false,
                isEnabled = notificationsAllowed
            )
        )
    }

    private fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    private fun exactAlarmStatusText(context: Context, enabled: Boolean): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            context.getString(R.string.profile_notifications_exact_alarm_not_required)
        } else {
            ProfilePreferenceStore.onOffText(context, enabled)
        }
    }

    private fun blockedUsersRows(blockedUsers: List<SocialUser>): List<ProfileDetailItem> {
        if (blockedUsers.isEmpty()) {
            return listOf(
                ProfileDetailItem(
                    titleResId = R.string.profile_blocked_users_empty,
                    subtitleResId = R.string.profile_blocked_users_empty_desc,
                    isInfoCard = true,
                    showChevron = false
                )
            )
        }

        val infoCard = ProfileDetailItem(
            titleResId = R.string.profile_blocked_users_info_title,
            subtitleResId = R.string.profile_blocked_users_info_desc,
            isInfoCard = true,
            showChevron = false,
            showDividerAfter = true
        )

        val userRows = blockedUsers.map { blockedUser ->
            val displayText = blockedUser.userName
                .takeIf { it.isNotBlank() }
                ?: blockedUser.displayName?.takeIf { it.isNotBlank() }

            ProfileDetailItem(
                titleResId = R.string.profile_blocked_user_deleted_title,
                titleText = displayText,
                subtitleResId = R.string.profile_blocked_user_unblock_hint,
                action = ProfileDetailAction.UNBLOCK_USER,
                relatedUserId = blockedUser.id
            )
        }

        return listOf(infoCard) + userRows
    }

    private fun connectedAppsRows(context: Context): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                titleResId = R.string.health_connect_title,
                subtitleResId = R.string.health_connect_subtitle,
                trailingText = context.getString(R.string.health_connect_connect),
                iconResId = R.drawable.ic_link_variant,
                action = ProfileDetailAction.CONNECT_HEALTH,
                showChevron = true
            )
        )
    }

    private fun helpFeedbackRows(): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_detail_contact_help,
                subtitleResId = R.string.profile_detail_contact_help_desc
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_contact_feedback,
                subtitleResId = R.string.profile_detail_contact_feedback_desc
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_contact_report,
                subtitleResId = R.string.profile_detail_contact_report_desc
            )
        )
    }

    private fun privacyPolicyRows(): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_intro_title,
                subtitleResId = R.string.profile_privacy_policy_intro_desc,
                isInfoCard = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_collected_data_title,
                subtitleResId = R.string.profile_privacy_policy_collected_data_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_usage_title,
                subtitleResId = R.string.profile_privacy_policy_usage_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_social_title,
                subtitleResId = R.string.profile_privacy_policy_social_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_messages_title,
                subtitleResId = R.string.profile_privacy_policy_messages_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_delete_title,
                subtitleResId = R.string.profile_privacy_policy_delete_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_services_title,
                subtitleResId = R.string.profile_privacy_policy_services_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_privacy_policy_security_title,
                subtitleResId = R.string.profile_privacy_policy_security_desc,
                showChevron = false
            )
        )
    }

    private fun termsRows(): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_terms_intro_title,
                subtitleResId = R.string.profile_terms_intro_desc,
                isInfoCard = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_account_title,
                subtitleResId = R.string.profile_terms_account_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_health_title,
                subtitleResId = R.string.profile_terms_health_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_social_title,
                subtitleResId = R.string.profile_terms_social_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_content_title,
                subtitleResId = R.string.profile_terms_content_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_blocking_title,
                subtitleResId = R.string.profile_terms_blocking_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_delete_title,
                subtitleResId = R.string.profile_terms_delete_desc,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_terms_changes_title,
                subtitleResId = R.string.profile_terms_changes_desc,
                showChevron = false
            )
        )
    }

    private fun notificationListInfoRows(): List<ProfileDetailItem> {
        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_menu_notifications,
                subtitleResId = R.string.profile_notifications_list_info,
                isInfoCard = true,
                showChevron = false
            )
        )
    }

    private fun achievementsRows(): List<ProfileDetailItem> = listOf(
        ProfileDetailItem(
            titleResId = R.string.profile_detail_achievements_badges,
            subtitleResId = R.string.profile_detail_achievements_badges_desc,
            iconResId = R.drawable.ic_star_outline,
            targetMenuId = ProfileMenuId.ACHIEVEMENT_BADGES
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_achievements_streak,
            subtitleResId = R.string.profile_detail_achievements_streak_desc,
            iconResId = R.drawable.ic_history,
            targetMenuId = ProfileMenuId.ACHIEVEMENT_STREAK
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_achievements_water,
            subtitleResId = R.string.profile_detail_achievements_water_desc,
            iconResId = R.drawable.ic_water_bottle,
            targetMenuId = ProfileMenuId.ACHIEVEMENT_WATER
        )
    )

    private fun achievementBadgesRows(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileDetailItem> {
        val weightProgress = healthData.weightGoalProgressPercent.coerceIn(0, 100)
        val calorieText = if (healthData.dailyTargetCalories > 0) {
            context.getString(R.string.profile_calorie_value_format, healthData.dailyTargetCalories)
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }

        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_achievement_badges_title,
                subtitleText = context.getString(R.string.profile_achievement_badges_intro),
                isInfoCard = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_badge_first_step_title),
                titleResId = R.string.profile_achievement_badge_first_step_title,
                subtitleText = context.getString(R.string.profile_achievement_badge_first_step_desc),
                trailingText = context.getString(R.string.profile_achievement_status_earned),
                iconResId = R.drawable.ic_check_circle,
                showChevron = false
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_badge_weight_title),
                titleResId = R.string.profile_achievement_badge_weight_title,
                subtitleText = context.getString(R.string.profile_achievement_badge_weight_desc, weightProgress),
                trailingText = context.getString(R.string.profile_percent_format, weightProgress),
                iconResId = R.drawable.ic_monitor_weight,
                showChevron = false
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_badge_goal_title),
                titleResId = R.string.profile_achievement_badge_goal_title,
                subtitleText = context.getString(R.string.profile_achievement_badge_goal_desc, calorieText),
                trailingText = if (healthData.dailyTargetCalories > 0) context.getString(R.string.profile_achievement_status_active) else context.getString(R.string.profile_achievement_status_locked),
                iconResId = R.drawable.ic_bullseye_arrow,
                showChevron = false
            )
        )
    }

    private fun achievementStreakRows(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileDetailItem> {
        val historyCount = healthData.history.size.coerceAtLeast(if (healthData.currentWeightKg > 0) 1 else 0)
        val streakDay = historyCount.coerceAtLeast(0)

        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_achievement_streak_title,
                subtitleText = context.getString(R.string.profile_achievement_streak_intro),
                isInfoCard = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_streak_current_title),
                titleResId = R.string.profile_achievement_streak_current_title,
                subtitleText = context.getString(R.string.profile_achievement_streak_current_desc),
                trailingText = context.getString(R.string.profile_achievement_day_format, streakDay),
                iconResId = R.drawable.ic_history,
                showChevron = false
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_streak_next_title),
                titleResId = R.string.profile_achievement_streak_next_title,
                subtitleText = context.getString(R.string.profile_achievement_streak_next_desc),
                trailingText = context.getString(R.string.profile_achievement_day_format, (7 - streakDay).coerceAtLeast(0)),
                iconResId = R.drawable.ic_schedule,
                showChevron = false
            )
        )
    }

    private fun achievementWaterRows(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileDetailItem> {
        val waterTarget = healthData.waterTargetLiter.takeIf { it > 0.0 } ?: DEFAULT_WATER_TARGET_LITER

        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_achievement_water_title,
                subtitleText = context.getString(R.string.profile_achievement_water_intro),
                isInfoCard = true,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_water_target_title),
                titleResId = R.string.profile_achievement_water_target_title,
                subtitleText = context.getString(R.string.profile_achievement_water_target_desc),
                trailingText = context.getString(R.string.profile_liter_value_format, waterTarget),
                iconResId = R.drawable.ic_water_bottle,
                showChevron = false
            ),
            ProfileDetailItem(
                titleText = context.getString(R.string.profile_achievement_water_consistency_title),
                titleResId = R.string.profile_achievement_water_consistency_title,
                subtitleText = context.getString(R.string.profile_achievement_water_consistency_desc),
                trailingText = context.getString(R.string.profile_achievement_status_active),
                iconResId = R.drawable.ic_check_circle,
                showChevron = false
            )
        )
    }

    private fun reminderRows(): List<ProfileDetailItem> = listOf(
        ProfileDetailItem(
            titleResId = R.string.profile_detail_reminders_meal,
            subtitleResId = R.string.profile_detail_reminders_meal_desc
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_reminders_water,
            subtitleResId = R.string.profile_detail_reminders_water_desc
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_reminders_weight,
            subtitleResId = R.string.profile_detail_reminders_weight_desc
        )
    )

    private fun albumRows(): List<ProfileDetailItem> = listOf(
        ProfileDetailItem(
            titleResId = R.string.profile_detail_album_camera,
            subtitleResId = R.string.profile_detail_album_camera_desc
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_album_photos,
            subtitleResId = R.string.profile_detail_album_photos_desc
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_album_filter,
            subtitleResId = R.string.profile_detail_album_filter_desc
        )
    )

    private fun privacyRows(context: Context): List<ProfileDetailItem> = listOf(
        ProfileDetailItem(
            titleResId = R.string.profile_detail_privacy_visibility,
            trailingText = ProfilePreferenceStore.accountVisibilityText(context),
            action = ProfileDetailAction.ACCOUNT_VISIBILITY
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_privacy_comments,
            trailingText = ProfilePreferenceStore.onOffText(
                context,
                ProfilePreferenceStore.areCommentsEnabled(context)
            ),
            action = ProfileDetailAction.TOGGLE_COMMENTS,
            isSwitch = true,
            switchChecked = ProfilePreferenceStore.areCommentsEnabled(context)
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_privacy_policy,
            subtitleResId = R.string.profile_detail_privacy_policy_desc,
            targetMenuId = ProfileMenuId.PRIVACY_POLICY
        )
    )

    private fun contactRows(): List<ProfileDetailItem> = listOf(
        ProfileDetailItem(
            titleResId = R.string.profile_detail_contact_help,
            subtitleResId = R.string.profile_detail_contact_help_desc
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_contact_feedback,
            subtitleResId = R.string.profile_detail_contact_feedback_desc
        ),
        ProfileDetailItem(
            titleResId = R.string.profile_detail_contact_report,
            subtitleResId = R.string.profile_detail_contact_report_desc
        )
    )

    private fun weightRows(
        context: Context,
        healthData: ProfileHealthData
    ): List<ProfileDetailItem> {
        val currentWeightText = if (healthData.currentWeightKg > 0) {
            context.getString(R.string.profile_weight_current_value_format, formatWeightKg(healthData.currentWeightKg))
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }

        val targetWeightText = if (healthData.targetWeightKg > 0) {
            context.getString(R.string.profile_weight_target_value_format, formatWeightKg(healthData.targetWeightKg))
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }

        val dailyTargetText = if (healthData.dailyTargetCalories > 0) {
            context.getString(R.string.profile_daily_target_calorie_format, healthData.dailyTargetCalories)
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }

        val estimatedTimeText = healthData.estimatedWeeks?.let { weeks ->
            context.getString(R.string.profile_estimated_weeks_format, weeks)
        } ?: context.getString(R.string.profile_data_not_saved_yet)

        return listOf(
            ProfileDetailItem(
                titleResId = R.string.profile_detail_weight_current,
                trailingText = currentWeightText,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_weight_target,
                trailingText = targetWeightText,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_weight_daily_target,
                trailingText = dailyTargetText,
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_weight_estimated_time,
                trailingText = estimatedTimeText,
                showChevron = false,
                showDividerAfter = true
            ),
            ProfileDetailItem(
                sectionTitleResId = R.string.profile_detail_group_inputs,
                titleResId = R.string.profile_detail_weight_height_age,
                subtitleText = buildHeightAgeText(context = context, healthData = healthData),
                showChevron = false
            ),
            ProfileDetailItem(
                titleResId = R.string.profile_detail_weight_history,
                subtitleResId = R.string.profile_detail_weight_history_desc
            )
        )
    }

    private fun buildHeightAgeText(
        context: Context,
        healthData: ProfileHealthData
    ): String {
        val heightText = if (healthData.heightCm > 0) {
            context.getString(R.string.profile_height_value_format, healthData.heightCm)
        } else {
            context.getString(R.string.profile_data_missing_short)
        }

        val ageText = if (healthData.age > 0) {
            context.getString(R.string.profile_age_value_format, healthData.age)
        } else {
            context.getString(R.string.profile_data_missing_short)
        }

        return context.getString(R.string.profile_height_age_format, heightText, ageText)
    }

    private fun gramText(context: Context, grams: Int): String {
        return if (grams > 0) {
            context.getString(R.string.profile_gram_value_format, grams)
        } else {
            context.getString(R.string.profile_data_not_saved_yet)
        }
    }
}
