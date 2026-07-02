package com.example.fitto.feature_home.presentation.profile

enum class ProfileNotificationToggle {
    MASTER,
    ACHIEVEMENT,
    REMINDER,
    SOCIAL,
    DIRECT_MESSAGE,
    GROUP_MESSAGE,
    VIBRATION
}

interface ProfileNotificationSettingsStore {
    fun isEnabled(toggle: ProfileNotificationToggle): Boolean
    fun setEnabled(toggle: ProfileNotificationToggle, enabled: Boolean)
    fun notificationSound(): ProfileNotificationSound
    fun setNotificationSound(sound: ProfileNotificationSound)
    fun setCustomNotificationSound(uri: String, displayName: String)
    fun silentHours(): String
    fun setSilentHours(value: String)
    fun markExactAlarmPromptSeen()
}
