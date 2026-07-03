package com.example.fitto.feature_home.presentation.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPermission
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_home.presentation.social.SocialUser

class ProfileDetailFragment : Fragment(R.layout.fragment_profile_detail) {

    private val menuId: ProfileMenuId by lazy {
        val rawValue = requireArguments().getString(ARG_MENU_ID)
        runCatching { ProfileMenuId.valueOf(rawValue.orEmpty()) }
            .getOrDefault(ProfileMenuId.SETTINGS)
    }

    fun currentMenuId(): ProfileMenuId = menuId

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: ProfileViewModel
    private val accountSettingsRepository = AccountSettingsRepository()
    private val socialRepository = SocialRepository()
    private var accountEditCoordinator: ProfileAccountEditCoordinator? = null
    private var accountDialogCoordinator: ProfileAccountDialogCoordinator? = null
    private var emailChangeDialogCoordinator: ProfileEmailChangeDialogCoordinator? = null
    private var accountLifecycleCoordinator: ProfileAccountLifecycleCoordinator? = null
    private var accountLifecycleDialogCoordinator: ProfileAccountLifecycleDialogCoordinator? = null
    private var notificationSettingsCoordinator: ProfileNotificationSettingsCoordinator? = null

    private var photoAlbumCoordinator: ProfilePhotoAlbumCoordinator? = null
    private var dataManagementCoordinator: ProfileDataManagementCoordinator? = null
    private var dataExportShareCoordinator: ProfileDataExportShareCoordinator? = null
    private var selectedPhotoAlbumDateKey: String = ProfilePhotoAlbumFilter.ALL
    private var pendingPhotoDocumentItem: ProfilePhotoAlbumItem? = null

    private var detailListRenderer: ProfileDetailListRenderer? = null
    private var preferenceDialogCoordinator: ProfilePreferenceDialogCoordinator? = null
    private var actionDialogCoordinator: ProfileActionDialogCoordinator? = null

    private var lastProfileErrorMessage: String? = null

    private val notificationSoundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        notificationSettingsCoordinator?.saveCustomNotificationSound(uri)
    }

    private val photoDocumentSaverLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        val item = pendingPhotoDocumentItem
        pendingPhotoDocumentItem = null

        if (uri != null && item != null) {
            photoAlbumCoordinator?.copyToDocument(item, uri)
        }
    }

    private var latestHealthData: ProfileHealthData = ProfileHealthData.Empty
    private var latestUser: SocialUser? = null
    private var latestBlockedUsers: List<SocialUser> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        setupDataManagement()
        setupAccountEditing()
        setupAccountLifecycle()
        setupNotificationSettings()

        val titleTextView = view.findViewById<TextView>(R.id.textProfileDetailTitle)
        val backButton = view.findViewById<TextView>(R.id.buttonProfileDetailBack)

        recyclerView = view.findViewById(R.id.profileDetailRecyclerView)
        val photoAlbumContent = view.findViewById<View>(R.id.profilePhotoAlbumContent)

        titleTextView.setText(ProfileDetailContent.titleFor(menuId))

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        detailListRenderer = ProfileDetailListRenderer(
            context = requireContext(),
            recyclerView = recyclerView,
            onItemClick = ::handleDetailItemClick,
            onAchievementTarget = ::openNestedProfileDetail
        )
        preferenceDialogCoordinator = ProfilePreferenceDialogCoordinator(
            context = requireContext(),
            inflater = layoutInflater,
            refreshRows = ::bindRows
        )
        actionDialogCoordinator = ProfileActionDialogCoordinator(
            context = requireContext(),
            inflater = layoutInflater
        )

        if (menuId == ProfileMenuId.PHOTO_ALBUM) {
            recyclerView.isVisible = false
            photoAlbumCoordinator = ProfilePhotoAlbumCoordinator(
                context = requireContext(),
                fragmentManager = childFragmentManager,
                scope = viewLifecycleOwner.lifecycleScope,
                posts = socialRepository.observeMyProfilePosts(),
                contentView = photoAlbumContent,
                recyclerView = view.findViewById(R.id.recyclerProfilePhotoAlbum),
                emptyTextView = view.findViewById(R.id.textProfileAlbumEmpty),
                dateFilterHintTextView = view.findViewById(R.id.textProfileAlbumDateFilterHint),
                dateFilterButton = view.findViewById(R.id.buttonProfileAlbumDateFilter),
                dateClearButton = view.findViewById(R.id.buttonProfileAlbumDateClear),
                initialSelectedDateKey = selectedPhotoAlbumDateKey,
                onSelectedDateKeyChanged = { dateKey -> selectedPhotoAlbumDateKey = dateKey },
                requestDocumentSave = { item, fileName ->
                    pendingPhotoDocumentItem = item
                    photoDocumentSaverLauncher.launch(fileName)
                },
                showMessage = ::showToast
            ).also { coordinator ->
                coordinator.start()
            }
        } else {
            recyclerView.isVisible = true
            photoAlbumContent.isVisible = false
        }

        observeProfileData()
        if (menuId != ProfileMenuId.PHOTO_ALBUM) {
            bindRows()
        }
        viewModel.loadProfile()

        if (menuId == ProfileMenuId.BLOCKED_USERS) {
            loadBlockedUsers()
        }
    }

    override fun onResume() {
        super.onResume()

        when (menuId) {
            ProfileMenuId.ACCOUNT_MANAGEMENT -> accountEditCoordinator?.refreshAccountEmail()
            ProfileMenuId.NOTIFICATION_PREFERENCES,
            ProfileMenuId.NUTRITION_PREFERENCES -> bindRows()
            else -> Unit
        }
    }

    override fun onDestroyView() {
        accountEditCoordinator?.clear()
        accountEditCoordinator = null
        accountDialogCoordinator?.clear()
        accountDialogCoordinator = null
        emailChangeDialogCoordinator?.clear()
        emailChangeDialogCoordinator = null
        accountLifecycleCoordinator?.clear()
        accountLifecycleCoordinator = null
        accountLifecycleDialogCoordinator?.clear()
        accountLifecycleDialogCoordinator = null
        notificationSettingsCoordinator?.clear()
        notificationSettingsCoordinator = null
        photoAlbumCoordinator?.clear()
        photoAlbumCoordinator = null
        dataManagementCoordinator?.clear()
        dataManagementCoordinator = null
        dataExportShareCoordinator = null
        preferenceDialogCoordinator?.clear()
        preferenceDialogCoordinator = null
        actionDialogCoordinator?.clear()
        actionDialogCoordinator = null
        detailListRenderer?.clear()
        detailListRenderer = null
        super.onDestroyView()
    }

    private fun setupDataManagement() {
        val appContext = requireContext().applicationContext

        dataExportShareCoordinator = ProfileDataExportShareCoordinator(
            context = requireContext(),
            subject = getString(R.string.profile_export_data_subject),
            chooserTitle = getString(R.string.profile_export_data_chooser),
            launchIntent = ::startActivity
        )

        dataManagementCoordinator = ProfileDataManagementCoordinator(
            scope = viewLifecycleOwner.lifecycleScope,
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
                showToast(R.string.profile_clear_history_success)
                viewModel.refreshProfile()
                bindRows()
            },
            onHistoryClearFailed = { error ->
                Log.e(TAG, "Clear history failed", error)
                showToast(R.string.profile_clear_history_error)
            },
            onExportReady = { file ->
                dataExportShareCoordinator?.share(file)
                showToast(R.string.profile_export_data_ready)
            },
            onExportFailed = { error ->
                Log.e(TAG, "Profile export failed", error)
                showToast(R.string.profile_export_data_error)
            },
            onBlockedUsersLoaded = { blockedUsers ->
                latestBlockedUsers = blockedUsers
                bindRows()
            },
            onBlockedUsersLoadFailed = { error ->
                Log.e(TAG, "Blocked users could not be loaded", error)
                showToast(R.string.profile_blocked_users_load_error)
            },
            onUserUnblocked = { result ->
                showToast(R.string.profile_unblock_user_success)
                latestBlockedUsers = result.remainingUsers
                bindRows()
            },
            onUserUnblockFailed = { error ->
                Log.e(TAG, "User could not be unblocked", error)
                showToast(R.string.profile_unblock_user_error)
            }
        )
    }

    private fun setupAccountEditing() {
        if (menuId != ProfileMenuId.ACCOUNT_MANAGEMENT) return

        val accountAction = ProfileAccountEditAction(
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
        )

        accountDialogCoordinator = ProfileAccountDialogCoordinator(
            context = requireContext(),
            inflater = layoutInflater
        )

        emailChangeDialogCoordinator = ProfileEmailChangeDialogCoordinator(
            context = requireContext(),
            inflater = layoutInflater,
            scope = viewLifecycleOwner.lifecycleScope,
            action = ProfileEmailChangeAction(
                checkAndSyncPendingEmailChange = accountSettingsRepository::checkAndSyncPendingEmailChange,
                signInAfterVerifiedEmailChange = accountSettingsRepository::signInAfterVerifiedEmailChange,
                resendPendingEmailChangeVerification =
                    accountSettingsRepository::resendPendingEmailChangeVerification
            ),
            languageCode = ::emailLanguageCode,
            onVerified = {
                latestUser = accountDisplayUser()
                bindRows()
                viewModel.refreshProfile()
            },
            onVerificationFinished = {
                showToast(R.string.profile_account_email_updated)
            },
            onCheckFailed = { error ->
                Log.w(TAG, "Email change verification check skipped", error)
            },
            onResendFailed = { error ->
                showToast(ProfileAccountErrorResolver.resolve(error))
            }
        )

        accountEditCoordinator = ProfileAccountEditCoordinator(
            scope = viewLifecycleOwner.lifecycleScope,
            action = accountAction,
            languageCode = ::emailLanguageCode,
            onDisplayNameUpdated = {
                showToast(R.string.profile_account_display_name_updated)
                viewModel.refreshProfile()
            },
            onUsernameUpdated = {
                showToast(R.string.profile_account_username_updated)
                viewModel.refreshProfile()
            },
            onEmailChangeRequested = { request ->
                emailChangeDialogCoordinator?.show(request)
            },
            onPasswordUpdated = {
                showToast(R.string.profile_account_password_updated)
            },
            onAccountEmailRefreshed = { result ->
                result.syncFailure?.let { error ->
                    Log.w(TAG, "Auth email sync skipped", error)
                }
                latestUser = ProfileAccountDisplayResolver.resolve(
                    user = latestUser,
                    authEmail = result.authEmail
                )
                bindRows()
                if (result.shouldRefreshProfile) {
                    viewModel.refreshProfile()
                }
            },
            onFailure = { error ->
                showToast(ProfileAccountErrorResolver.resolve(error))
            }
        )
    }

    private fun setupAccountLifecycle() {
        if (menuId != ProfileMenuId.SETTINGS) return

        accountLifecycleDialogCoordinator = ProfileAccountLifecycleDialogCoordinator(
            context = requireContext(),
            inflater = layoutInflater
        )

        accountLifecycleCoordinator = ProfileAccountLifecycleCoordinator(
            scope = viewLifecycleOwner.lifecycleScope,
            action = ProfileAccountLifecycleActionFactory(requireContext()).create(),
            onSessionMissing = ::navigateToWelcome,
            onPasswordAuthenticationRequired = {
                accountLifecycleDialogCoordinator?.showPasswordReauthentication { password ->
                    accountLifecycleCoordinator?.reauthenticatePassword(password)
                }
            },
            onGoogleAuthenticationRequired = {
                accountLifecycleDialogCoordinator?.showGoogleReauthentication {
                    accountLifecycleCoordinator?.reauthenticateGoogle()
                }
            },
            onUnsupportedProvider = {
                showToast(R.string.profile_delete_account_provider_not_supported)
            },
            onReauthenticated = {
                accountLifecycleDialogCoordinator?.showFinalDeleteConfirmation {
                    accountLifecycleCoordinator?.deleteAccount(
                        deletedUserName = getString(R.string.deleted_user_display_name),
                        deletedMessageText = getString(R.string.deleted_message_text)
                    )
                }
            },
            onPasswordRejected = { error ->
                Log.e(TAG, "Account delete reauth failed: wrong password", error)
                showToast(R.string.profile_delete_account_wrong_password)
            },
            onPasswordAuthenticationFailed = { error ->
                error?.let {
                    Log.e(TAG, "Account delete password reauth failed", it)
                }
                showToast(
                    if (error == null) {
                        R.string.profile_delete_account_error
                    } else {
                        R.string.profile_delete_account_auth_error
                    }
                )
            },
            onGoogleAuthenticationFailed = { error ->
                Log.e(TAG, "Account delete Google reauth failed", error)
                showToast(R.string.profile_delete_account_google_error)
            },
            onAccountDeleted = {
                accountLifecycleDialogCoordinator?.dismissDeleteConfirmation()
                showToast(R.string.profile_delete_account_success)
                navigateToWelcome()
            },
            onDeleteRequiresRecentLogin = { error ->
                Log.e(TAG, "Firebase Auth account delete requires recent login", error)
                accountLifecycleDialogCoordinator?.restoreDeleteConfirmation()
                showToast(R.string.profile_delete_account_requires_recent_login)
            },
            onDeleteFailed = { error ->
                Log.e(TAG, "Firebase Auth account delete failed", error)
                accountLifecycleDialogCoordinator?.restoreDeleteConfirmation()
                showToast(R.string.profile_delete_account_error)
            },
            onLoggedOut = ::navigateToWelcome
        )
    }

    private fun setupNotificationSettings() {
        if (menuId != ProfileMenuId.NOTIFICATION_PREFERENCES) return

        val appContext = requireContext().applicationContext
        val store = AndroidProfileNotificationSettingsStore(appContext)
        val dialogCoordinator = ProfileNotificationSettingsDialogCoordinator(
            context = requireContext(),
            inflater = layoutInflater,
            fragmentManager = childFragmentManager,
            store = store,
            openCustomSoundPicker = {
                notificationSoundPickerLauncher.launch(arrayOf("audio/*"))
            },
            refreshRows = ::bindRows,
            showMessage = ::showToast,
            launchIntent = ::startActivity
        )

        notificationSettingsCoordinator = ProfileNotificationSettingsCoordinator(
            action = ProfileNotificationSettingsAction(
                store = store,
                requestNotificationPermission = {
                    FittoNotificationPermission.requestIfNeeded(requireActivity())
                },
                scheduleReminders = {
                    ReminderAlarmScheduler.scheduleAll(appContext)
                },
                cancelReminders = {
                    ReminderAlarmScheduler.cancelAll(appContext)
                }
            ),
            dialogCoordinator = dialogCoordinator,
            refreshRows = ::bindRows
        )
    }

    private fun accountDisplayUser(): SocialUser? {
        return ProfileAccountDisplayResolver.resolve(
            user = latestUser,
            authEmail = accountSettingsRepository.currentAuthEmail()
        )
    }

    private fun observeProfileData() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            latestHealthData = state.healthData
            latestUser = state.user ?: latestUser
            latestUser = accountDisplayUser()

            if (menuId == ProfileMenuId.PHOTO_ALBUM) {
                photoAlbumCoordinator?.updateProfilePhoto(latestUser?.photoUrl)
            } else {
                bindRows()
            }

            val errorMessage = state.errorMessage
            val shouldSuppressSessionError = menuId == ProfileMenuId.ACCOUNT_MANAGEMENT &&
                    emailChangeDialogCoordinator?.isRecoveryActive == true &&
                    errorMessage == MISSING_SESSION_ERROR_MESSAGE

            if (errorMessage.isNullOrBlank() || shouldSuppressSessionError) {
                if (!shouldSuppressSessionError) {
                    lastProfileErrorMessage = null
                }
            } else if (errorMessage != lastProfileErrorMessage) {
                lastProfileErrorMessage = errorMessage
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindRows() {
        detailListRenderer?.render(
            menuId = menuId,
            healthData = latestHealthData,
            user = accountDisplayUser(),
            blockedUsers = latestBlockedUsers
        )
    }

    private fun handleDetailItemClick(item: ProfileDetailItem) {
        item.targetMenuId?.let { targetMenuId ->
            openNestedProfileDetail(targetMenuId)
            return
        }

        when (item.action) {
            ProfileDetailAction.REGION -> preferenceDialogCoordinator?.showRegion()
            ProfileDetailAction.LANGUAGE -> preferenceDialogCoordinator?.showLanguage()
            ProfileDetailAction.THEME -> preferenceDialogCoordinator?.showTheme()
            ProfileDetailAction.ACCOUNT_VISIBILITY -> preferenceDialogCoordinator?.showAccountVisibility()
            ProfileDetailAction.EDIT_EMAIL -> showUpdateEmailDialog()
            ProfileDetailAction.EDIT_USERNAME -> showUpdateUsernameDialog()
            ProfileDetailAction.EDIT_DISPLAY_NAME -> showUpdateDisplayNameDialog()
            ProfileDetailAction.CHANGE_PASSWORD -> showChangePasswordDialog()
            ProfileDetailAction.NOTIFICATION_SOUND -> notificationSettingsCoordinator?.showNotificationSoundDialog()
            ProfileDetailAction.SILENT_HOURS -> notificationSettingsCoordinator?.showSilentHoursDialog()
            ProfileDetailAction.TOGGLE_COMMENTS -> toggleComments()
            ProfileDetailAction.UNBLOCK_USER -> showUnblockUserDialog(item)
            ProfileDetailAction.TOGGLE_NOTIFICATIONS_ALLOWED -> toggleNotification(ProfileNotificationToggle.MASTER)
            ProfileDetailAction.TOGGLE_ACHIEVEMENT_NOTIFICATIONS -> toggleNotification(ProfileNotificationToggle.ACHIEVEMENT)
            ProfileDetailAction.TOGGLE_REMINDER_NOTIFICATIONS -> toggleNotification(ProfileNotificationToggle.REMINDER)
            ProfileDetailAction.TOGGLE_SOCIAL_NOTIFICATIONS -> toggleNotification(ProfileNotificationToggle.SOCIAL)
            ProfileDetailAction.TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS -> toggleNotification(ProfileNotificationToggle.DIRECT_MESSAGE)
            ProfileDetailAction.TOGGLE_GROUP_MESSAGE_NOTIFICATIONS -> toggleNotification(ProfileNotificationToggle.GROUP_MESSAGE)
            ProfileDetailAction.TOGGLE_VIBRATION -> toggleNotification(ProfileNotificationToggle.VIBRATION)
            ProfileDetailAction.EXACT_ALARM_PERMISSION -> notificationSettingsCoordinator?.showExactAlarmPermissionDialog()

            ProfileDetailAction.CONNECT_HEALTH -> {
                HealthConnectBottomSheetFragment()
                    .show(childFragmentManager, "profile_health_connect")
            }

            ProfileDetailAction.LOGOUT -> {
                accountLifecycleDialogCoordinator?.showLogoutConfirmation {
                    accountLifecycleCoordinator?.logout()
                }
            }

            ProfileDetailAction.CLEAR_HISTORY -> actionDialogCoordinator?.showConfirmation(
                titleResId = R.string.profile_detail_clear_history,
                messageResId = R.string.profile_clear_history_confirm_message,
                positiveResId = R.string.profile_detail_clear_history,
                iconResId = R.drawable.ic_swipe_delete,
                iconColorResId = R.color.profile_subtext,
                onConfirmed = ::clearProfileHistory
            )

            ProfileDetailAction.EXPORT_DATA -> exportProfileData()
            ProfileDetailAction.DELETE_ACCOUNT -> {
                accountLifecycleDialogCoordinator?.showDeleteIntroduction {
                    accountLifecycleCoordinator?.beginDeleteAuthentication()
                }
            }
            ProfileDetailAction.RECALCULATE_GOALS -> showToast(R.string.profile_daily_goals_recalculate_coming_soon)
            ProfileDetailAction.NUTRITION_DIET_TYPES -> preferenceDialogCoordinator?.showNutritionDietTypes()
            ProfileDetailAction.NUTRITION_ALLERGIES ->
                preferenceDialogCoordinator?.showNutritionAllergies()
            ProfileDetailAction.NUTRITION_DISLIKED_FOODS ->
                preferenceDialogCoordinator?.showNutritionDislikedFoods()
            ProfileDetailAction.NUTRITION_MEAL_ROUTINE -> preferenceDialogCoordinator?.showNutritionMealRoutine()
            null -> showToast(item.titleResId)
        }
    }


    private fun clearProfileHistory() {
        dataManagementCoordinator?.clearHistory()
    }

    private fun exportProfileData() {
        val state = viewModel.uiState.value
        dataManagementCoordinator?.exportData(
            ProfileDataExportRequest(
                user = latestUser,
                healthData = latestHealthData,
                bodyMeasurements = state?.bodyMeasurements ?: BodyMeasurementsData(),
                bodyMeasurementHistory = state?.bodyMeasurementsHistory.orEmpty()
            )
        )
    }

    private fun openNestedProfileDetail(targetMenuId: ProfileMenuId) {
        (parentFragment as? ProfileScreenNavigator)
            ?.openProfileDetailScreen(targetMenuId)
    }

    private fun toggleComments() {
        val enabled = !ProfilePreferenceStore.areCommentsEnabled(requireContext())
        ProfilePreferenceStore.setCommentsEnabled(requireContext(), enabled)
        bindRows()
    }

    private fun loadBlockedUsers() {
        dataManagementCoordinator?.loadBlockedUsers()
    }

    private fun showUnblockUserDialog(item: ProfileDetailItem) {
        val userId = item.relatedUserId

        if (userId.isNullOrBlank()) {
            showToast(R.string.profile_blocked_users_load_error)
            return
        }

        actionDialogCoordinator?.show(
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
                unblockUser(userId)
            }
        )
    }

    private fun unblockUser(userId: String) {
        dataManagementCoordinator?.unblockUser(
            userId = userId,
            currentUsers = latestBlockedUsers
        )
    }

    private fun emailLanguageCode(): String {
        return when (ProfilePreferenceStore.getLanguage(requireContext())) {
            ProfileLanguage.TURKISH -> "tr"
            ProfileLanguage.ENGLISH -> "en"
        }
    }

    private fun toggleNotification(toggle: ProfileNotificationToggle) {
        notificationSettingsCoordinator?.toggle(toggle)
    }

    private fun showUpdateDisplayNameDialog() {
        accountDialogCoordinator?.showDisplayName(
            initialValue = latestUser?.personalName.orEmpty(),
            onConfirmed = { displayName ->
                accountEditCoordinator?.updateDisplayName(displayName)
            }
        )
    }

    private fun showUpdateUsernameDialog() {
        accountDialogCoordinator?.showUsername(
            initialValue = latestUser?.userName.orEmpty(),
            onConfirmed = { username ->
                accountEditCoordinator?.updateUsername(username)
            }
        )
    }

    private fun showUpdateEmailDialog() {
        val currentEmail = accountEditCoordinator
            ?.currentEmail()
            .orEmpty()
            .ifBlank { latestUser?.email.orEmpty() }

        accountDialogCoordinator?.showEmail(
            initialValue = currentEmail,
            onConfirmed = ::continueUpdateEmailAuthentication
        )
    }

    private fun continueUpdateEmailAuthentication(newEmail: String) {
        when (accountEditCoordinator?.emailAuthenticationRoute()) {
            ProfileAccountAuthenticationRoute.PASSWORD -> {
                accountDialogCoordinator?.showEmailPassword { password ->
                    accountEditCoordinator?.requestEmailChange(
                        newEmail = newEmail,
                        password = password
                    )
                }
            }

            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED -> {
                accountDialogCoordinator?.showGoogleManagedAccount()
            }

            ProfileAccountAuthenticationRoute.UNSUPPORTED,
            null -> showToast(R.string.profile_account_provider_not_supported)
        }
    }

    private fun showChangePasswordDialog() {
        when (accountEditCoordinator?.passwordAuthenticationRoute()) {
            ProfileAccountAuthenticationRoute.PASSWORD -> {
                accountDialogCoordinator?.showPasswordChange {
                        currentPassword,
                        newPassword,
                        confirmPassword ->
                    accountEditCoordinator?.updatePassword(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword
                    )
                }
            }

            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED -> {
                accountDialogCoordinator?.showGoogleManagedAccount()
            }

            ProfileAccountAuthenticationRoute.UNSUPPORTED,
            null -> showToast(R.string.profile_account_provider_not_supported)
        }
    }

    private fun navigateToWelcome() {
        val navController = Navigation.findNavController(requireActivity(), R.id.mainNavHost)
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()

        navController.navigate(R.id.welcomeFragment, null, navOptions)
    }

    private fun showToast(@StringRes messageResId: Int) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
    }


    companion object {
        private const val ARG_MENU_ID = "menu_id"
        private const val MISSING_SESSION_ERROR_MESSAGE = "Oturum bilgisi bulunamadı."
        private const val TAG = "ProfileDetailFragment"

        fun newInstance(menuId: ProfileMenuId): ProfileDetailFragment {
            return ProfileDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MENU_ID, menuId.name)
                }
            }
        }
    }

}
