package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.FragmentManager
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_home.presentation.social.SocialUser
import kotlinx.coroutines.CoroutineScope

internal data class ProfileDetailCoordinatorCallbacks(
    val refreshRows: () -> Unit,
    val refreshProfile: () -> Unit,
    val currentUser: () -> SocialUser?,
    val updateCurrentUser: (SocialUser?) -> Unit,
    val updateBlockedUsers: (List<SocialUser>) -> Unit,
    val navigateToWelcome: () -> Unit,
    val emailLanguageCode: () -> String,
    val requestNotificationPermission: () -> Boolean,
    val openNotificationSoundPicker: () -> Unit,
    val launchIntent: (Intent) -> Unit,
    val showMessage: (Int) -> Unit
)

internal class ProfileDetailCoordinatorFactory(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val fragmentManager: FragmentManager,
    private val scope: CoroutineScope,
    private val menuId: ProfileMenuId,
    private val callbacks: ProfileDetailCoordinatorCallbacks,
    private val accountSettingsRepository: AccountSettingsRepository = AccountSettingsRepository(),
    private val socialRepository: SocialRepository = SocialRepository()
) {
    private val appContext = context.applicationContext
    private val featureScope = ProfileDetailFeatureScopeResolver.resolve(menuId)

    fun create(): ProfileDetailCoordinatorSet {
        val preferenceDialogs = ProfilePreferenceDialogCoordinator(
            context = context,
            inflater = inflater,
            refreshRows = callbacks.refreshRows
        )
        val actionDialogs = ProfileActionDialogCoordinator(
            context = context,
            inflater = inflater
        )
        val dataManagement = createDataManagementCoordinator()
        val accountEditing = createAccountEditingCoordinators()
        val accountLifecycle = createAccountLifecycleCoordinators()
        val notificationSettings = createNotificationSettingsCoordinator()

        return ProfileDetailCoordinatorSet(
            dataManagement = dataManagement,
            accountEdit = accountEditing?.accountEdit,
            accountDialogs = accountEditing?.accountDialogs,
            emailChangeDialogs = accountEditing?.emailChangeDialogs,
            accountLifecycle = accountLifecycle?.accountLifecycle,
            accountLifecycleDialogs = accountLifecycle?.accountLifecycleDialogs,
            notificationSettings = notificationSettings,
            preferenceDialogs = preferenceDialogs,
            actionDialogs = actionDialogs,
            currentAuthEmail = accountSettingsRepository::currentAuthEmail
        )
    }

    private fun createDataManagementCoordinator(): ProfileDataManagementCoordinator {
        val shareCoordinator = ProfileDataExportShareCoordinator(
            context = context,
            subject = context.getString(R.string.profile_export_data_subject),
            chooserTitle = context.getString(R.string.profile_export_data_chooser),
            launchIntent = callbacks.launchIntent
        )

        return ProfileDataManagementCoordinator(
            scope = scope,
            historyClearAction = ProfileHistoryClearAction(
                clearRemoteHistory = accountSettingsRepository::clearCurrentUserHistory,
                clearLocalHistory = { userId ->
                    ProfileDataCleaner.clearCurrentUserData(appContext, userId)
                },
                clearNotifications = {
                    FittoNotificationStore.clearCurrentAccountNotifications(appContext)
                }
            ),
            dataExportAction = ProfileDataExportAction(appContext),
            blockedUsersAction = ProfileBlockedUsersAction(
                getBlockedUsers = socialRepository::getBlockedUsers,
                unblockUser = { userId -> socialRepository.unblockUser(userId) }
            ),
            onHistoryCleared = {
                callbacks.showMessage(R.string.profile_clear_history_success)
                callbacks.refreshProfile()
                callbacks.refreshRows()
            },
            onHistoryClearFailed = { error ->
                Log.e(TAG, "Clear history failed", error)
                callbacks.showMessage(R.string.profile_clear_history_error)
            },
            onExportReady = { file ->
                shareCoordinator.share(file)
                callbacks.showMessage(R.string.profile_export_data_ready)
            },
            onExportFailed = { error ->
                Log.e(TAG, "Profile export failed", error)
                callbacks.showMessage(R.string.profile_export_data_error)
            },
            onBlockedUsersLoaded = { blockedUsers ->
                callbacks.updateBlockedUsers(blockedUsers)
                callbacks.refreshRows()
            },
            onBlockedUsersLoadFailed = { error ->
                Log.e(TAG, "Blocked users could not be loaded", error)
                callbacks.showMessage(R.string.profile_blocked_users_load_error)
            },
            onUserUnblocked = { result ->
                callbacks.showMessage(R.string.profile_unblock_user_success)
                callbacks.updateBlockedUsers(result.remainingUsers)
                callbacks.refreshRows()
            },
            onUserUnblockFailed = { error ->
                Log.e(TAG, "User could not be unblocked", error)
                callbacks.showMessage(R.string.profile_unblock_user_error)
            }
        )
    }

    private fun createAccountEditingCoordinators(): AccountEditingCoordinators? {
        if (!featureScope.usesAccountEditing) return null

        val accountDialogs = ProfileAccountDialogCoordinator(
            context = context,
            inflater = inflater
        )
        val emailChangeDialogs = ProfileEmailChangeDialogCoordinator(
            context = context,
            inflater = inflater,
            scope = scope,
            action = ProfileEmailChangeAction(
                checkAndSyncPendingEmailChange =
                    accountSettingsRepository::checkAndSyncPendingEmailChange,
                signInAfterVerifiedEmailChange =
                    accountSettingsRepository::signInAfterVerifiedEmailChange,
                resendPendingEmailChangeVerification =
                    accountSettingsRepository::resendPendingEmailChangeVerification
            ),
            languageCode = callbacks.emailLanguageCode,
            onVerified = {
                updateDisplayedUser(accountSettingsRepository.currentAuthEmail())
                callbacks.refreshRows()
                callbacks.refreshProfile()
            },
            onVerificationFinished = {
                callbacks.showMessage(R.string.profile_account_email_updated)
            },
            onCheckFailed = { error ->
                Log.w(TAG, "Email change verification check skipped", error)
            },
            onResendFailed = { error ->
                callbacks.showMessage(ProfileAccountErrorResolver.resolve(error))
            }
        )

        val accountEdit = ProfileAccountEditCoordinator(
            scope = scope,
            action = ProfileAccountEditAction(
                updateDisplayName = accountSettingsRepository::updateDisplayName,
                updateUsername = accountSettingsRepository::updateUsername,
                reauthenticateWithPassword = accountSettingsRepository::reauthenticateWithPassword,
                updateEmailAfterReauth = accountSettingsRepository::updateEmailAfterReauth,
                updatePasswordAfterReauth = accountSettingsRepository::updatePasswordAfterReauth,
                syncCurrentAuthEmail = {
                    accountSettingsRepository.syncCurrentAuthEmailToProfileIfNeeded()
                },
                reloadCurrentUserBestEffort = accountSettingsRepository::reloadCurrentUserBestEffort,
                currentAuthEmail = accountSettingsRepository::currentAuthEmail,
                hasCurrentUser = accountSettingsRepository::hasCurrentUser,
                isPasswordProvider = accountSettingsRepository::isPasswordProvider,
                isGoogleProvider = accountSettingsRepository::isGoogleProvider
            ),
            languageCode = callbacks.emailLanguageCode,
            onDisplayNameUpdated = {
                callbacks.showMessage(R.string.profile_account_display_name_updated)
                callbacks.refreshProfile()
            },
            onUsernameUpdated = {
                callbacks.showMessage(R.string.profile_account_username_updated)
                callbacks.refreshProfile()
            },
            onEmailChangeRequested = emailChangeDialogs::show,
            onPasswordUpdated = {
                callbacks.showMessage(R.string.profile_account_password_updated)
            },
            onAccountEmailRefreshed = { result ->
                result.syncFailure?.let { error ->
                    Log.w(TAG, "Auth email sync skipped", error)
                }
                updateDisplayedUser(result.authEmail)
                callbacks.refreshRows()
                if (result.shouldRefreshProfile) {
                    callbacks.refreshProfile()
                }
            },
            onFailure = { error ->
                callbacks.showMessage(ProfileAccountErrorResolver.resolve(error))
            }
        )

        return AccountEditingCoordinators(
            accountEdit = accountEdit,
            accountDialogs = accountDialogs,
            emailChangeDialogs = emailChangeDialogs
        )
    }

    private fun createAccountLifecycleCoordinators(): AccountLifecycleCoordinators? {
        if (!featureScope.usesAccountLifecycle) return null

        val dialogs = ProfileAccountLifecycleDialogCoordinator(
            context = context,
            inflater = inflater
        )
        lateinit var coordinator: ProfileAccountLifecycleCoordinator
        coordinator = ProfileAccountLifecycleCoordinator(
            scope = scope,
            action = ProfileAccountLifecycleActionFactory(context).create(),
            onSessionMissing = callbacks.navigateToWelcome,
            onPasswordAuthenticationRequired = {
                dialogs.showPasswordReauthentication(coordinator::reauthenticatePassword)
            },
            onGoogleAuthenticationRequired = {
                dialogs.showGoogleReauthentication(coordinator::reauthenticateGoogle)
            },
            onUnsupportedProvider = {
                callbacks.showMessage(R.string.profile_delete_account_provider_not_supported)
            },
            onReauthenticated = {
                dialogs.showFinalDeleteConfirmation {
                    coordinator.deleteAccount(
                        deletedUserName = context.getString(R.string.deleted_user_display_name),
                        deletedMessageText = context.getString(R.string.deleted_message_text)
                    )
                }
            },
            onPasswordRejected = { error ->
                Log.e(TAG, "Account delete reauth failed: wrong password", error)
                callbacks.showMessage(R.string.profile_delete_account_wrong_password)
            },
            onPasswordAuthenticationFailed = { error ->
                error?.let {
                    Log.e(TAG, "Account delete password reauth failed", it)
                }
                callbacks.showMessage(
                    if (error == null) {
                        R.string.profile_delete_account_error
                    } else {
                        R.string.profile_delete_account_auth_error
                    }
                )
            },
            onGoogleAuthenticationFailed = { error ->
                Log.e(TAG, "Account delete Google reauth failed", error)
                callbacks.showMessage(R.string.profile_delete_account_google_error)
            },
            onAccountDeleted = {
                dialogs.dismissDeleteConfirmation()
                callbacks.showMessage(R.string.profile_delete_account_success)
                callbacks.navigateToWelcome()
            },
            onDeleteRequiresRecentLogin = { error ->
                Log.e(TAG, "Firebase Auth account delete requires recent login", error)
                dialogs.restoreDeleteConfirmation()
                callbacks.showMessage(R.string.profile_delete_account_requires_recent_login)
            },
            onDeleteFailed = { error ->
                Log.e(TAG, "Firebase Auth account delete failed", error)
                dialogs.restoreDeleteConfirmation()
                callbacks.showMessage(R.string.profile_delete_account_error)
            },
            onLoggedOut = callbacks.navigateToWelcome
        )

        return AccountLifecycleCoordinators(
            accountLifecycle = coordinator,
            accountLifecycleDialogs = dialogs
        )
    }

    private fun createNotificationSettingsCoordinator(): ProfileNotificationSettingsCoordinator? {
        if (!featureScope.usesNotificationSettings) return null

        val store = AndroidProfileNotificationSettingsStore(appContext)
        val dialogs = ProfileNotificationSettingsDialogCoordinator(
            context = context,
            inflater = inflater,
            fragmentManager = fragmentManager,
            store = store,
            openCustomSoundPicker = callbacks.openNotificationSoundPicker,
            refreshRows = callbacks.refreshRows,
            showMessage = callbacks.showMessage,
            launchIntent = callbacks.launchIntent
        )

        return ProfileNotificationSettingsCoordinator(
            action = ProfileNotificationSettingsAction(
                store = store,
                requestNotificationPermission = callbacks.requestNotificationPermission,
                scheduleReminders = { ReminderAlarmScheduler.scheduleAll(appContext) },
                cancelReminders = { ReminderAlarmScheduler.cancelAll(appContext) }
            ),
            dialogCoordinator = dialogs,
            refreshRows = callbacks.refreshRows
        )
    }

    private fun updateDisplayedUser(authEmail: String) {
        callbacks.updateCurrentUser(
            ProfileAccountDisplayResolver.resolve(
                user = callbacks.currentUser(),
                authEmail = authEmail
            )
        )
    }

    private data class AccountEditingCoordinators(
        val accountEdit: ProfileAccountEditCoordinator,
        val accountDialogs: ProfileAccountDialogCoordinator,
        val emailChangeDialogs: ProfileEmailChangeDialogCoordinator
    )

    private data class AccountLifecycleCoordinators(
        val accountLifecycle: ProfileAccountLifecycleCoordinator,
        val accountLifecycleDialogs: ProfileAccountLifecycleDialogCoordinator
    )

    private companion object {
        const val TAG = "ProfileDetailCoordinatorFactory"
    }
}
