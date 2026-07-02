package com.example.fitto.feature_home.presentation.profile

import android.net.Uri

class ProfileNotificationSettingsCoordinator(
    private val action: ProfileNotificationSettingsAction,
    private val dialogCoordinator: ProfileNotificationSettingsDialogCoordinator,
    private val refreshRows: () -> Unit
) {

    fun toggle(toggle: ProfileNotificationToggle): ProfileNotificationToggleResult {
        return action.toggle(toggle).also { refreshRows() }
    }

    fun showNotificationSoundDialog() {
        dialogCoordinator.showNotificationSoundDialog()
    }

    fun saveCustomNotificationSound(uri: Uri) {
        dialogCoordinator.saveCustomNotificationSound(uri)
    }

    fun showSilentHoursDialog() {
        dialogCoordinator.showSilentHoursDialog()
    }

    fun showExactAlarmPermissionDialog() {
        dialogCoordinator.showExactAlarmPermissionDialog()
    }

    fun clear() {
        dialogCoordinator.clear()
    }
}
