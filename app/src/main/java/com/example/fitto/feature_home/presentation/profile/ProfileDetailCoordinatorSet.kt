package com.example.fitto.feature_home.presentation.profile

internal class ProfileDetailCoordinatorSet(
    val dataManagement: ProfileDataManagementCoordinator,
    val accountEdit: ProfileAccountEditCoordinator?,
    val accountDialogs: ProfileAccountDialogCoordinator?,
    val emailChangeDialogs: ProfileEmailChangeDialogCoordinator?,
    val accountLifecycle: ProfileAccountLifecycleCoordinator?,
    val accountLifecycleDialogs: ProfileAccountLifecycleDialogCoordinator?,
    val notificationSettings: ProfileNotificationSettingsCoordinator?,
    val preferenceDialogs: ProfilePreferenceDialogCoordinator,
    val actionDialogs: ProfileActionDialogCoordinator,
    val currentAuthEmail: () -> String
) {
    fun clear() {
        accountEdit?.clear()
        accountDialogs?.clear()
        emailChangeDialogs?.clear()
        accountLifecycle?.clear()
        accountLifecycleDialogs?.clear()
        notificationSettings?.clear()
        dataManagement.clear()
        preferenceDialogs.clear()
        actionDialogs.clear()
    }
}
