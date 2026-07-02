package com.example.fitto.feature_home.presentation.profile

import android.content.Context

class AndroidProfileNotificationSettingsStore(
    private val context: Context
) : ProfileNotificationSettingsStore {

    override fun isEnabled(toggle: ProfileNotificationToggle): Boolean {
        return when (toggle) {
            ProfileNotificationToggle.MASTER ->
                ProfilePreferenceStore.areNotificationsAllowed(context)

            ProfileNotificationToggle.ACHIEVEMENT ->
                ProfilePreferenceStore.areAchievementNotificationsEnabled(context)

            ProfileNotificationToggle.REMINDER ->
                ProfilePreferenceStore.areReminderNotificationsEnabled(context)

            ProfileNotificationToggle.SOCIAL ->
                ProfilePreferenceStore.areSocialNotificationsEnabled(context)

            ProfileNotificationToggle.DIRECT_MESSAGE ->
                ProfilePreferenceStore.areDirectMessageNotificationsEnabled(context)

            ProfileNotificationToggle.GROUP_MESSAGE ->
                ProfilePreferenceStore.areGroupMessageNotificationsEnabled(context)

            ProfileNotificationToggle.VIBRATION ->
                ProfilePreferenceStore.isVibrationEnabled(context)
        }
    }

    override fun setEnabled(toggle: ProfileNotificationToggle, enabled: Boolean) {
        when (toggle) {
            ProfileNotificationToggle.MASTER ->
                ProfilePreferenceStore.setNotificationsAllowed(context, enabled)

            ProfileNotificationToggle.ACHIEVEMENT ->
                ProfilePreferenceStore.setAchievementNotificationsEnabled(context, enabled)

            ProfileNotificationToggle.REMINDER ->
                ProfilePreferenceStore.setReminderNotificationsEnabled(context, enabled)

            ProfileNotificationToggle.SOCIAL ->
                ProfilePreferenceStore.setSocialNotificationsEnabled(context, enabled)

            ProfileNotificationToggle.DIRECT_MESSAGE ->
                ProfilePreferenceStore.setDirectMessageNotificationsEnabled(context, enabled)

            ProfileNotificationToggle.GROUP_MESSAGE ->
                ProfilePreferenceStore.setGroupMessageNotificationsEnabled(context, enabled)

            ProfileNotificationToggle.VIBRATION ->
                ProfilePreferenceStore.setVibrationEnabled(context, enabled)
        }
    }

    override fun notificationSound(): ProfileNotificationSound {
        return ProfilePreferenceStore.getNotificationSound(context)
    }

    override fun setNotificationSound(sound: ProfileNotificationSound) {
        ProfilePreferenceStore.setNotificationSound(context, sound)
    }

    override fun setCustomNotificationSound(uri: String, displayName: String) {
        ProfilePreferenceStore.setCustomNotificationSound(
            context = context,
            uri = uri,
            displayName = displayName
        )
    }

    override fun silentHours(): String {
        return ProfilePreferenceStore.silentHoursText(context)
    }

    override fun setSilentHours(value: String) {
        ProfilePreferenceStore.setSilentHoursText(context, value)
    }

    override fun markExactAlarmPromptSeen() {
        ProfilePreferenceStore.setExactAlarmPromptSeen(context)
    }
}
