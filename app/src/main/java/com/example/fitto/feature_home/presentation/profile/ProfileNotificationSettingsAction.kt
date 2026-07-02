package com.example.fitto.feature_home.presentation.profile

data class ProfileNotificationToggleResult(
    val toggle: ProfileNotificationToggle,
    val enabled: Boolean,
    val permissionRequested: Boolean,
    val remindersScheduled: Boolean,
    val remindersCancelled: Boolean
)

class ProfileNotificationSettingsAction(
    private val store: ProfileNotificationSettingsStore,
    private val requestNotificationPermission: () -> Boolean,
    private val scheduleReminders: () -> Unit,
    private val cancelReminders: () -> Unit
) {

    fun toggle(toggle: ProfileNotificationToggle): ProfileNotificationToggleResult {
        val enabled = !store.isEnabled(toggle)
        store.setEnabled(toggle, enabled)

        var permissionRequested = false
        var remindersScheduled = false
        var remindersCancelled = false

        when (toggle) {
            ProfileNotificationToggle.MASTER,
            ProfileNotificationToggle.REMINDER -> {
                if (enabled) {
                    permissionRequested = true
                    if (requestNotificationPermission()) {
                        scheduleReminders()
                        remindersScheduled = true
                    }
                } else {
                    cancelReminders()
                    remindersCancelled = true
                }
            }

            ProfileNotificationToggle.SOCIAL,
            ProfileNotificationToggle.DIRECT_MESSAGE,
            ProfileNotificationToggle.GROUP_MESSAGE -> {
                if (enabled) {
                    requestNotificationPermission()
                    permissionRequested = true
                }
            }

            ProfileNotificationToggle.ACHIEVEMENT,
            ProfileNotificationToggle.VIBRATION -> Unit
        }

        return ProfileNotificationToggleResult(
            toggle = toggle,
            enabled = enabled,
            permissionRequested = permissionRequested,
            remindersScheduled = remindersScheduled,
            remindersCancelled = remindersCancelled
        )
    }
}
