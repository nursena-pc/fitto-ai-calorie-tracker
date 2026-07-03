package com.example.fitto.feature_home.presentation.profile

internal object ProfileDetailNotificationToggleResolver {
    fun resolve(action: ProfileDetailAction?): ProfileNotificationToggle? {
        return when (action) {
            ProfileDetailAction.TOGGLE_NOTIFICATIONS_ALLOWED -> ProfileNotificationToggle.MASTER
            ProfileDetailAction.TOGGLE_ACHIEVEMENT_NOTIFICATIONS -> ProfileNotificationToggle.ACHIEVEMENT
            ProfileDetailAction.TOGGLE_REMINDER_NOTIFICATIONS -> ProfileNotificationToggle.REMINDER
            ProfileDetailAction.TOGGLE_SOCIAL_NOTIFICATIONS -> ProfileNotificationToggle.SOCIAL
            ProfileDetailAction.TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS -> ProfileNotificationToggle.DIRECT_MESSAGE
            ProfileDetailAction.TOGGLE_GROUP_MESSAGE_NOTIFICATIONS -> ProfileNotificationToggle.GROUP_MESSAGE
            ProfileDetailAction.TOGGLE_VIBRATION -> ProfileNotificationToggle.VIBRATION
            else -> null
        }
    }
}
