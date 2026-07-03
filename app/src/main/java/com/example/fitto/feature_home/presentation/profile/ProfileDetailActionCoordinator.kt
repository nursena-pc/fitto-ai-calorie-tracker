package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.R
import com.example.fitto.feature_home.presentation.social.SocialUser

internal class ProfileDetailActionCoordinator(
    private val coordinators: ProfileDetailCoordinatorSet,
    private val currentUser: () -> SocialUser?,
    private val currentBlockedUsers: () -> List<SocialUser>,
    private val exportRequest: () -> ProfileDataExportRequest,
    private val openNestedProfileDetail: (ProfileMenuId) -> Unit,
    private val openHealthConnect: () -> Unit,
    private val toggleComments: () -> Unit,
    private val showMessage: (Int) -> Unit
) {
    fun handle(item: ProfileDetailItem) {
        item.targetMenuId?.let { targetMenuId ->
            openNestedProfileDetail(targetMenuId)
            return
        }

        ProfileDetailNotificationToggleResolver.resolve(item.action)?.let { toggle ->
            coordinators.notificationSettings?.toggle(toggle)
            return
        }

        when (item.action) {
            ProfileDetailAction.REGION -> coordinators.preferenceDialogs.showRegion()
            ProfileDetailAction.LANGUAGE -> coordinators.preferenceDialogs.showLanguage()
            ProfileDetailAction.THEME -> coordinators.preferenceDialogs.showTheme()
            ProfileDetailAction.ACCOUNT_VISIBILITY ->
                coordinators.preferenceDialogs.showAccountVisibility()

            ProfileDetailAction.EDIT_EMAIL -> showUpdateEmailDialog()
            ProfileDetailAction.EDIT_USERNAME -> showUpdateUsernameDialog()
            ProfileDetailAction.EDIT_DISPLAY_NAME -> showUpdateDisplayNameDialog()
            ProfileDetailAction.CHANGE_PASSWORD -> showChangePasswordDialog()
            ProfileDetailAction.NOTIFICATION_SOUND ->
                coordinators.notificationSettings?.showNotificationSoundDialog()

            ProfileDetailAction.SILENT_HOURS ->
                coordinators.notificationSettings?.showSilentHoursDialog()

            ProfileDetailAction.TOGGLE_COMMENTS -> toggleComments()
            ProfileDetailAction.UNBLOCK_USER -> showUnblockUserDialog(item.relatedUserId)
            ProfileDetailAction.EXACT_ALARM_PERMISSION ->
                coordinators.notificationSettings?.showExactAlarmPermissionDialog()

            ProfileDetailAction.CONNECT_HEALTH -> openHealthConnect()
            ProfileDetailAction.LOGOUT -> showLogoutConfirmation()
            ProfileDetailAction.CLEAR_HISTORY -> showClearHistoryConfirmation()
            ProfileDetailAction.EXPORT_DATA -> coordinators.dataManagement.exportData(exportRequest())
            ProfileDetailAction.DELETE_ACCOUNT -> showDeleteAccountIntroduction()
            ProfileDetailAction.RECALCULATE_GOALS ->
                showMessage(R.string.profile_daily_goals_recalculate_coming_soon)

            ProfileDetailAction.NUTRITION_DIET_TYPES ->
                coordinators.preferenceDialogs.showNutritionDietTypes()

            ProfileDetailAction.NUTRITION_ALLERGIES ->
                coordinators.preferenceDialogs.showNutritionAllergies()

            ProfileDetailAction.NUTRITION_DISLIKED_FOODS ->
                coordinators.preferenceDialogs.showNutritionDislikedFoods()

            ProfileDetailAction.NUTRITION_MEAL_ROUTINE ->
                coordinators.preferenceDialogs.showNutritionMealRoutine()

            ProfileDetailAction.TOGGLE_NOTIFICATIONS_ALLOWED,
            ProfileDetailAction.TOGGLE_ACHIEVEMENT_NOTIFICATIONS,
            ProfileDetailAction.TOGGLE_REMINDER_NOTIFICATIONS,
            ProfileDetailAction.TOGGLE_SOCIAL_NOTIFICATIONS,
            ProfileDetailAction.TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS,
            ProfileDetailAction.TOGGLE_GROUP_MESSAGE_NOTIFICATIONS,
            ProfileDetailAction.TOGGLE_VIBRATION -> Unit

            null -> showMessage(item.titleResId)
        }
    }

    private fun showUpdateDisplayNameDialog() {
        coordinators.accountDialogs?.showDisplayName(
            initialValue = currentUser()?.personalName.orEmpty(),
            onConfirmed = { displayName ->
                coordinators.accountEdit?.updateDisplayName(displayName)
            }
        )
    }

    private fun showUpdateUsernameDialog() {
        coordinators.accountDialogs?.showUsername(
            initialValue = currentUser()?.userName.orEmpty(),
            onConfirmed = { username ->
                coordinators.accountEdit?.updateUsername(username)
            }
        )
    }

    private fun showUpdateEmailDialog() {
        val currentEmail = coordinators.accountEdit
            ?.currentEmail()
            .orEmpty()
            .ifBlank { currentUser()?.email.orEmpty() }

        coordinators.accountDialogs?.showEmail(
            initialValue = currentEmail,
            onConfirmed = ::continueUpdateEmailAuthentication
        )
    }

    private fun continueUpdateEmailAuthentication(newEmail: String) {
        when (coordinators.accountEdit?.emailAuthenticationRoute()) {
            ProfileAccountAuthenticationRoute.PASSWORD -> {
                coordinators.accountDialogs?.showEmailPassword { password ->
                    coordinators.accountEdit?.requestEmailChange(
                        newEmail = newEmail,
                        password = password
                    )
                }
            }

            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED -> {
                coordinators.accountDialogs?.showGoogleManagedAccount()
            }

            ProfileAccountAuthenticationRoute.UNSUPPORTED,
            null -> showMessage(R.string.profile_account_provider_not_supported)
        }
    }

    private fun showChangePasswordDialog() {
        when (coordinators.accountEdit?.passwordAuthenticationRoute()) {
            ProfileAccountAuthenticationRoute.PASSWORD -> {
                coordinators.accountDialogs?.showPasswordChange {
                        currentPassword,
                        newPassword,
                        confirmPassword ->
                    coordinators.accountEdit?.updatePassword(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword
                    )
                }
            }

            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED -> {
                coordinators.accountDialogs?.showGoogleManagedAccount()
            }

            ProfileAccountAuthenticationRoute.UNSUPPORTED,
            null -> showMessage(R.string.profile_account_provider_not_supported)
        }
    }

    private fun showLogoutConfirmation() {
        coordinators.accountLifecycleDialogs?.showLogoutConfirmation {
            coordinators.accountLifecycle?.logout()
        }
    }

    private fun showDeleteAccountIntroduction() {
        coordinators.accountLifecycleDialogs?.showDeleteIntroduction {
            coordinators.accountLifecycle?.beginDeleteAuthentication()
        }
    }

    private fun showClearHistoryConfirmation() {
        coordinators.actionDialogs.showConfirmation(
            titleResId = R.string.profile_detail_clear_history,
            messageResId = R.string.profile_clear_history_confirm_message,
            positiveResId = R.string.profile_detail_clear_history,
            iconResId = R.drawable.ic_swipe_delete,
            iconColorResId = R.color.profile_subtext,
            onConfirmed = coordinators.dataManagement::clearHistory
        )
    }

    private fun showUnblockUserDialog(userId: String?) {
        if (userId.isNullOrBlank()) {
            showMessage(R.string.profile_blocked_users_load_error)
            return
        }

        coordinators.actionDialogs.show(
            titleResId = R.string.profile_unblock_user_title,
            warningResId = null,
            messageResId = R.string.profile_unblock_user_message,
            positiveResId = R.string.profile_unblock_user_confirm,
            negativeResId = R.string.profile_delete_account_cancel,
            iconResId = R.drawable.ic_person_outline,
            iconColorResId = R.color.fitto_green,
            confirmTextColorResId = R.color.fitto_green,
            showPasswordInput = false,
            onConfirmed = {
                coordinators.dataManagement.unblockUser(
                    userId = userId,
                    currentUsers = currentBlockedUsers()
                )
            }
        )
    }
}
