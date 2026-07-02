package com.example.fitto.feature_home.presentation.profile

import android.app.AlarmManager
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPermission
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetCountryPickerBinding
import com.example.fitto.databinding.ItemCountryBinding
import com.example.fitto.feature_auth.login.GoogleSignInHelper
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_home.presentation.social.SocialUser
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
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

    private var photoAlbumCoordinator: ProfilePhotoAlbumCoordinator? = null
    private var dataManagementCoordinator: ProfileDataManagementCoordinator? = null
    private var dataExportShareCoordinator: ProfileDataExportShareCoordinator? = null
    private var selectedPhotoAlbumDateKey: String = ProfilePhotoAlbumFilter.ALL
    private var pendingPhotoDocumentItem: ProfilePhotoAlbumItem? = null

    private val achievementAdapter: ProfileAchievementAdapter by lazy {
        ProfileAchievementAdapter { item ->
            item.targetMenuId?.let { targetMenuId -> openNestedProfileDetail(targetMenuId) }
        }
    }

    private var lastProfileErrorMessage: String? = null

    private val notificationSoundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        saveCustomNotificationSound(uri)
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

        val titleTextView = view.findViewById<TextView>(R.id.textProfileDetailTitle)
        val backButton = view.findViewById<TextView>(R.id.buttonProfileDetailBack)

        recyclerView = view.findViewById(R.id.profileDetailRecyclerView)
        val photoAlbumContent = view.findViewById<View>(R.id.profilePhotoAlbumContent)

        titleTextView.setText(ProfileDetailContent.titleFor(menuId))

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

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
        photoAlbumCoordinator?.clear()
        photoAlbumCoordinator = null
        dataManagementCoordinator?.clear()
        dataManagementCoordinator = null
        dataExportShareCoordinator = null
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
        if (!::recyclerView.isInitialized) return

        if (isAchievementMenu(menuId)) {
            bindAchievementRows()
            return
        }

        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val firstVisiblePosition = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
        val firstVisibleTop = if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            layoutManager?.findViewByPosition(firstVisiblePosition)?.top ?: 0
        } else {
            0
        }

        val rows = ProfileDetailContent.rowsFor(
            context = requireContext(),
            menuId = menuId,
            healthData = latestHealthData,
            user = accountDisplayUser(),
            blockedUsers = latestBlockedUsers
        )

        val currentAdapter = recyclerView.adapter as? ProfileDetailAdapter
        if (currentAdapter == null) {
            recyclerView.adapter = ProfileDetailAdapter(
                items = rows,
                onItemClick = { item -> handleDetailItemClick(item) }
            )
        } else {
            currentAdapter.updateItems(rows)
        }

        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            recyclerView.post {
                (recyclerView.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(firstVisiblePosition, firstVisibleTop)
            }
        }
    }


    private fun bindAchievementRows() {
        if (recyclerView.adapter !== achievementAdapter) {
            recyclerView.adapter = achievementAdapter
        }

        achievementAdapter.submitList(
            ProfileAchievementContent.itemsFor(
                context = requireContext(),
                menuId = menuId,
                healthData = latestHealthData
            )
        )
    }

    private fun handleDetailItemClick(item: ProfileDetailItem) {
        item.targetMenuId?.let { targetMenuId ->
            openNestedProfileDetail(targetMenuId)
            return
        }

        when (item.action) {
            ProfileDetailAction.REGION -> showRegionDialog()
            ProfileDetailAction.LANGUAGE -> showLanguageDialog()
            ProfileDetailAction.THEME -> showThemeDialog()
            ProfileDetailAction.ACCOUNT_VISIBILITY -> showAccountVisibilityDialog()
            ProfileDetailAction.EDIT_EMAIL -> showUpdateEmailDialog()
            ProfileDetailAction.EDIT_USERNAME -> showUpdateUsernameDialog()
            ProfileDetailAction.EDIT_DISPLAY_NAME -> showUpdateDisplayNameDialog()
            ProfileDetailAction.CHANGE_PASSWORD -> showChangePasswordDialog()
            ProfileDetailAction.NOTIFICATION_SOUND -> showNotificationSoundDialog()
            ProfileDetailAction.SILENT_HOURS -> showSilentHoursDialog()
            ProfileDetailAction.TOGGLE_COMMENTS -> toggleComments()
            ProfileDetailAction.UNBLOCK_USER -> showUnblockUserDialog(item)
            ProfileDetailAction.TOGGLE_NOTIFICATIONS_ALLOWED -> toggleNotificationsAllowed()
            ProfileDetailAction.TOGGLE_ACHIEVEMENT_NOTIFICATIONS -> toggleAchievementNotifications()
            ProfileDetailAction.TOGGLE_REMINDER_NOTIFICATIONS -> toggleReminderNotifications()
            ProfileDetailAction.TOGGLE_SOCIAL_NOTIFICATIONS -> toggleSocialNotifications()
            ProfileDetailAction.TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS -> toggleDirectMessageNotifications()
            ProfileDetailAction.TOGGLE_GROUP_MESSAGE_NOTIFICATIONS -> toggleGroupMessageNotifications()
            ProfileDetailAction.TOGGLE_VIBRATION -> toggleVibration()
            ProfileDetailAction.EXACT_ALARM_PERMISSION -> showExactAlarmPermissionDialogFromSettings()

            ProfileDetailAction.CONNECT_HEALTH -> {
                HealthConnectBottomSheetFragment()
                    .show(childFragmentManager, "profile_health_connect")
            }

            ProfileDetailAction.LOGOUT -> showConfirmDialog(
                titleResId = R.string.profile_detail_settings_logout,
                messageResId = R.string.profile_logout_confirm_message,
                positiveResId = R.string.profile_detail_settings_logout,
                iconResId = R.drawable.ic_logout,
                iconColorResId = R.color.profile_subtext,
                confirmTextColorResId = R.color.reminder_delete,
                onConfirmed = ::performLogout
            )

            ProfileDetailAction.CLEAR_HISTORY -> showConfirmDialog(
                titleResId = R.string.profile_detail_clear_history,
                messageResId = R.string.profile_clear_history_confirm_message,
                positiveResId = R.string.profile_detail_clear_history,
                iconResId = R.drawable.ic_swipe_delete,
                iconColorResId = R.color.profile_subtext,
                onConfirmed = ::clearProfileHistory
            )

            ProfileDetailAction.EXPORT_DATA -> exportProfileData()
            ProfileDetailAction.DELETE_ACCOUNT -> showDeleteAccountDialog()
            ProfileDetailAction.RECALCULATE_GOALS -> showToast(R.string.profile_daily_goals_recalculate_coming_soon)
            ProfileDetailAction.NUTRITION_DIET_TYPES -> showNutritionDietTypesDialog()
            ProfileDetailAction.NUTRITION_ALLERGIES -> showNutritionTextDialog(
                titleResId = R.string.nutrition_pref_allergies_title,
                messageResId = R.string.nutrition_pref_allergies_desc,
                labelResId = R.string.nutrition_pref_allergies_title,
                hintResId = R.string.nutrition_pref_allergies_hint,
                initialValue = ProfilePreferenceStore.getNutritionAllergies(requireContext()),
                onConfirmed = { value ->
                    ProfilePreferenceStore.setNutritionAllergies(requireContext(), value)
                    bindRows()
                }
            )
            ProfileDetailAction.NUTRITION_DISLIKED_FOODS -> showNutritionTextDialog(
                titleResId = R.string.nutrition_pref_disliked_title,
                messageResId = R.string.nutrition_pref_disliked_desc,
                labelResId = R.string.nutrition_pref_disliked_title,
                hintResId = R.string.nutrition_pref_disliked_hint,
                initialValue = ProfilePreferenceStore.getNutritionDislikedFoods(requireContext()),
                onConfirmed = { value ->
                    ProfilePreferenceStore.setNutritionDislikedFoods(requireContext(), value)
                    bindRows()
                }
            )
            ProfileDetailAction.NUTRITION_MEAL_ROUTINE -> showNutritionMealRoutineDialog()
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

    private fun showNutritionDietTypesDialog() {
        val current = ProfilePreferenceStore.getNutritionDietPreferences(requireContext()).toMutableSet()
        val options = NutritionDietPreference.entries
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_choice, null, false)
        val titleTextView = dialogView.findViewById<TextView>(R.id.textChoiceDialogTitle)
        val optionsLayout = dialogView.findViewById<LinearLayout>(R.id.layoutChoiceOptions)
        val cancelButton = dialogView.findViewById<TextView>(R.id.buttonChoiceDialogCancel)

        titleTextView.setText(R.string.nutrition_pref_diet_type_title)
        cancelButton.setText(R.string.common_save)
        cancelButton.setTextColor(requireContext().getColor(R.color.fitto_green))
        optionsLayout.removeAllViews()

        fun refresh() {
            optionsLayout.removeAllViews()
            options.forEach { option ->
                val optionView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_profile_choice, optionsLayout, false)
                val optionTitle = optionView.findViewById<TextView>(R.id.textChoiceTitle)
                val checkText = optionView.findViewById<TextView>(R.id.textChoiceCheck)
                optionTitle.text = ProfilePreferenceStore.nutritionDietPreferenceText(requireContext(), option)
                checkText.visibility = if (current.contains(option)) View.VISIBLE else View.INVISIBLE
                optionView.setOnClickListener {
                    if (option == NutritionDietPreference.STANDARD) {
                        current.clear()
                        current += NutritionDietPreference.STANDARD
                    } else {
                        current.remove(NutritionDietPreference.STANDARD)
                        if (current.contains(option)) current.remove(option) else current.add(option)
                        if (current.isEmpty()) current += NutritionDietPreference.STANDARD
                    }
                    refresh()
                }
                optionsLayout.addView(optionView)
            }
        }

        cancelButton.setOnClickListener {
            ProfilePreferenceStore.setNutritionDietPreferences(requireContext(), current)
            dialog.dismiss()
            bindRows()
        }

        refresh()
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showNutritionMealRoutineDialog() {
        val current = ProfilePreferenceStore.getNutritionMealRoutine(requireContext())
        showFittoChoiceDialog(
            titleResId = R.string.nutrition_pref_meal_routine_title,
            options = listOf(
                ChoiceOption(
                    title = getString(R.string.nutrition_pref_routine_three_meals),
                    value = NutritionMealRoutine.THREE_MEALS,
                    isSelected = current == NutritionMealRoutine.THREE_MEALS
                ),
                ChoiceOption(
                    title = getString(R.string.nutrition_pref_routine_two_meals_snacks),
                    value = NutritionMealRoutine.TWO_MEALS_SNACKS,
                    isSelected = current == NutritionMealRoutine.TWO_MEALS_SNACKS
                ),
                ChoiceOption(
                    title = getString(R.string.nutrition_pref_routine_flexible),
                    value = NutritionMealRoutine.FLEXIBLE,
                    isSelected = current == NutritionMealRoutine.FLEXIBLE
                )
            ),
            onSelected = { routine ->
                ProfilePreferenceStore.setNutritionMealRoutine(requireContext(), routine)
                bindRows()
            }
        )
    }

    private fun showNutritionTextDialog(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes labelResId: Int,
        @StringRes hintResId: Int,
        initialValue: String,
        onConfirmed: (String) -> Unit
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_fitto_action, null, false)

        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val inputArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val inputLabel = dialogView.findViewById<TextView>(R.id.textPasswordLabel)
        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutPasswordInput)
        val inputEditText = dialogView.findViewById<EditText>(R.id.editActionPassword)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        iconImage.setImageResource(R.drawable.ic_nutrition_fork)
        iconImage.setColorFilter(requireContext().getColor(R.color.fitto_green))
        titleText.setText(titleResId)
        messageText.setText(messageResId)
        warningText.isVisible = false
        inputArea.isVisible = true
        inputLabel.setText(labelResId)
        inputLayout.hint = getString(hintResId)
        inputLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        inputEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        inputEditText.setSingleLine(false)
        inputEditText.minLines = 1
        inputEditText.maxLines = 3
        applySoftInputHintStyle(inputLayout, inputEditText)
        inputEditText.setText(initialValue)
        inputEditText.setSelection(inputEditText.text?.length ?: 0)

        cancelButton.setText(R.string.common_clear)
        confirmButton.setText(R.string.common_save)
        cancelButton.setTextColor(requireContext().getColor(R.color.profile_subtext))
        confirmButton.setTextColor(requireContext().getColor(R.color.fitto_green))

        cancelButton.setOnClickListener {
            inputEditText.setText("")
        }
        confirmButton.setOnClickListener {
            dialog.dismiss()
            onConfirmed(inputEditText.text?.toString().orEmpty())
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showRegionDialog() {
        val currentRegion = ProfilePreferenceStore.getRegion(requireContext())

        val turkey = getString(R.string.region_suggested_turkey)
        val english = getString(R.string.region_suggested_english)
        val other = getString(R.string.region_other)

        showFittoChoiceDialog(
            titleResId = R.string.profile_detail_settings_region,
            options = listOf(
                ChoiceOption(
                    title = turkey,
                    value = turkey,
                    isSelected = currentRegion == turkey
                ),
                ChoiceOption(
                    title = english,
                    value = english,
                    isSelected = currentRegion == english
                ),
                ChoiceOption(
                    title = other,
                    value = other,
                    isSelected = currentRegion != turkey && currentRegion != english
                )
            ),
            onSelected = { selectedRegion ->
                if (selectedRegion == other) {
                    showCountryPickerSheet()
                } else {
                    ProfilePreferenceStore.setRegion(
                        context = requireContext(),
                        region = selectedRegion
                    )
                    bindRows()
                }
            }
        )
    }

    private fun showLanguageDialog() {
        val currentLanguage = ProfilePreferenceStore.getLanguage(requireContext())

        showFittoChoiceDialog(
            titleResId = R.string.profile_detail_settings_language,
            options = listOf(
                ChoiceOption(
                    title = getString(R.string.language_turkish),
                    value = ProfileLanguage.TURKISH,
                    isSelected = currentLanguage == ProfileLanguage.TURKISH
                ),
                ChoiceOption(
                    title = getString(R.string.language_english),
                    value = ProfileLanguage.ENGLISH,
                    isSelected = currentLanguage == ProfileLanguage.ENGLISH
                )
            ),
            onSelected = { selectedLanguage ->
                ProfilePreferenceStore.setLanguage(
                    context = requireContext(),
                    language = selectedLanguage
                )
                bindRows()
            }
        )
    }

    private fun showThemeDialog() {
        val currentTheme = ProfilePreferenceStore.getTheme(requireContext())

        showFittoChoiceDialog(
            titleResId = R.string.profile_detail_settings_theme,
            options = listOf(
                ChoiceOption(
                    title = getString(R.string.profile_theme_system),
                    value = FittoThemeMode.SYSTEM,
                    isSelected = currentTheme == FittoThemeMode.SYSTEM
                ),
                ChoiceOption(
                    title = getString(R.string.profile_theme_light),
                    value = FittoThemeMode.LIGHT,
                    isSelected = currentTheme == FittoThemeMode.LIGHT
                ),
                ChoiceOption(
                    title = getString(R.string.profile_theme_dark),
                    value = FittoThemeMode.DARK,
                    isSelected = currentTheme == FittoThemeMode.DARK
                )
            ),
            onSelected = { selectedTheme ->
                ProfilePreferenceStore.setTheme(
                    context = requireContext(),
                    theme = selectedTheme
                )
                bindRows()
            }
        )
    }

    private fun showAccountVisibilityDialog() {
        val currentVisibility = ProfilePreferenceStore.getAccountVisibility(requireContext())

        showFittoChoiceDialog(
            titleResId = R.string.profile_detail_privacy_visibility,
            options = listOf(
                ChoiceOption(
                    title = getString(R.string.profile_visibility_public),
                    value = ProfileAccountVisibility.PUBLIC,
                    isSelected = currentVisibility == ProfileAccountVisibility.PUBLIC
                ),
                ChoiceOption(
                    title = getString(R.string.profile_visibility_followers),
                    value = ProfileAccountVisibility.FOLLOWERS,
                    isSelected = currentVisibility == ProfileAccountVisibility.FOLLOWERS
                ),
                ChoiceOption(
                    title = getString(R.string.profile_visibility_private),
                    value = ProfileAccountVisibility.PRIVATE,
                    isSelected = currentVisibility == ProfileAccountVisibility.PRIVATE
                )
            ),
            onSelected = { selectedVisibility ->
                ProfilePreferenceStore.setAccountVisibility(requireContext(), selectedVisibility)
                bindRows()
            }
        )
    }

    private fun showNotificationSoundDialog() {
        val currentSound = ProfilePreferenceStore.getNotificationSound(requireContext())

        showFittoChoiceDialog(
            titleResId = R.string.profile_notifications_sound,
            options = listOf(
                ChoiceOption(
                    title = getString(R.string.profile_notification_sound_soft),
                    value = ProfileNotificationSound.SOFT,
                    isSelected = currentSound == ProfileNotificationSound.SOFT
                ),
                ChoiceOption(
                    title = getString(R.string.profile_notification_sound_classic),
                    value = ProfileNotificationSound.CLASSIC,
                    isSelected = currentSound == ProfileNotificationSound.CLASSIC
                ),
                ChoiceOption(
                    title = getString(R.string.profile_notification_sound_nature),
                    value = ProfileNotificationSound.NATURE,
                    isSelected = currentSound == ProfileNotificationSound.NATURE
                ),
                ChoiceOption(
                    title = getString(R.string.profile_notification_sound_pick_from_device),
                    value = ProfileNotificationSound.CUSTOM,
                    isSelected = currentSound == ProfileNotificationSound.CUSTOM
                ),
                ChoiceOption(
                    title = getString(R.string.profile_notification_sound_silent),
                    value = ProfileNotificationSound.SILENT,
                    isSelected = currentSound == ProfileNotificationSound.SILENT
                )
            ),
            onSelected = { selectedSound ->
                if (selectedSound == ProfileNotificationSound.CUSTOM) {
                    notificationSoundPickerLauncher.launch(arrayOf("audio/*"))
                } else {
                    ProfilePreferenceStore.setNotificationSound(requireContext(), selectedSound)
                    bindRows()
                }
            }
        )
    }

    private fun saveCustomNotificationSound(uri: Uri) {
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        ProfilePreferenceStore.setCustomNotificationSound(
            context = requireContext(),
            uri = uri.toString(),
            displayName = displayNameForUri(uri)
        )
        bindRows()
    }

    private fun displayNameForUri(uri: Uri): String {
        val fallback = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_notification_sound_custom)

        return runCatching {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)?.takeIf { it.isNotBlank() } ?: fallback
                } else {
                    fallback
                }
            } ?: fallback
        }.getOrDefault(fallback)
    }

    private fun showSilentHoursDialog() {
        val currentSilentHours = ProfilePreferenceStore.silentHoursText(requireContext())
        val defaultSilentHours = getString(R.string.profile_silent_hours_default)
        val offSilentHours = getString(R.string.profile_silent_hours_off)
        val chooseRangeValue = "__choose_range__"

        showFittoChoiceDialog(
            titleResId = R.string.profile_notifications_silent_hours,
            options = listOf(
                ChoiceOption(
                    title = defaultSilentHours,
                    value = defaultSilentHours,
                    isSelected = currentSilentHours == defaultSilentHours
                ),
                ChoiceOption(
                    title = getString(R.string.profile_silent_hours_choose_range),
                    value = chooseRangeValue,
                    isSelected = currentSilentHours != defaultSilentHours && currentSilentHours != offSilentHours
                ),
                ChoiceOption(
                    title = offSilentHours,
                    value = offSilentHours,
                    isSelected = currentSilentHours == offSilentHours
                )
            ),
            onSelected = { selectedSilentHours ->
                if (selectedSilentHours == chooseRangeValue) {
                    showSilentHoursRangePicker(currentSilentHours)
                } else {
                    ProfilePreferenceStore.setSilentHoursText(requireContext(), selectedSilentHours)
                    bindRows()
                }
            }
        )
    }

    private fun showSilentHoursRangePicker(currentValue: String) {
        val startTime = silentHourPart(currentValue, startPart = true) ?: "22:00"
        val endTime = silentHourPart(currentValue, startPart = false) ?: "07:00"
        val startHour = startTime.substringBefore(":").toIntOrNull() ?: 22
        val startMinute = startTime.substringAfter(":").toIntOrNull() ?: 0
        val endHour = endTime.substringBefore(":").toIntOrNull() ?: 7
        val endMinute = endTime.substringAfter(":").toIntOrNull() ?: 0

        showTimePicker(
            initialHour = startHour,
            initialMinute = startMinute,
            title = getString(R.string.profile_silent_hours_start_time)
        ) { selectedStartHour, selectedStartMinute ->
            val startText = formatSilentHour(selectedStartHour, selectedStartMinute)
            showTimePicker(
                initialHour = endHour,
                initialMinute = endMinute,
                title = getString(R.string.profile_silent_hours_end_time)
            ) { selectedEndHour, selectedEndMinute ->
                val endText = formatSilentHour(selectedEndHour, selectedEndMinute)
                ProfilePreferenceStore.setSilentHoursText(requireContext(), "$startText - $endText")
                bindRows()
            }
        }
    }

    private fun showTimePicker(
        initialHour: Int,
        initialMinute: Int,
        title: String,
        onTimeSelected: (hour: Int, minute: Int) -> Unit
    ) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText(title)
            .setTheme(R.style.ThemeOverlay_Fitto_WhiteTimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(childFragmentManager, "fitto_silent_hours_time_picker")
    }

    private fun silentHourPart(value: String, startPart: Boolean): String? {
        if (!value.contains("-")) return null
        val part = if (startPart) {
            value.substringBefore("-").trim()
        } else {
            value.substringAfter("-").trim()
        }
        return part.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) }
    }

    private fun formatSilentHour(hour: Int, minute: Int): String {
        return "%02d:%02d".format(Locale.US, hour, minute)
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

        showFittoActionDialog(
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

    private fun showExactAlarmPermissionDialogFromSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            showToast(R.string.profile_notifications_exact_alarm_not_required)
            return
        }

        val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        if (alarmManager?.canScheduleExactAlarms() == true) {
            showToast(R.string.profile_notifications_exact_alarm_already_enabled)
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reminder_exact_alarm_permission_title)
            .setMessage(R.string.reminder_exact_alarm_permission_message)
            .setPositiveButton(R.string.reminder_exact_alarm_permission_action) { _, _ ->
                ProfilePreferenceStore.setExactAlarmPromptSeen(requireContext())
                openExactAlarmPermissionSettings()
            }
            .setNegativeButton(R.string.reminder_exact_alarm_permission_later) { _, _ ->
                ProfilePreferenceStore.setExactAlarmPromptSeen(requireContext())
            }
            .show()
    }

    private fun openExactAlarmPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${requireContext().packageName}")
        )
        runCatching { startActivity(intent) }
            .onFailure {
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${requireContext().packageName}")
                        )
                    )
                }
            }
    }

    private fun toggleNotificationsAllowed() {
        val enabled = !ProfilePreferenceStore.areNotificationsAllowed(requireContext())
        ProfilePreferenceStore.setNotificationsAllowed(requireContext(), enabled)

        if (enabled) {
            if (FittoNotificationPermission.requestIfNeeded(requireActivity())) {
                ReminderAlarmScheduler.scheduleAll(requireContext())
            }
        } else {
            ReminderAlarmScheduler.cancelAll(requireContext())
        }

        bindRows()
    }

    private fun toggleAchievementNotifications() {
        val enabled = !ProfilePreferenceStore.areAchievementNotificationsEnabled(requireContext())
        ProfilePreferenceStore.setAchievementNotificationsEnabled(requireContext(), enabled)
        bindRows()
    }

    private fun toggleReminderNotifications() {
        val enabled = !ProfilePreferenceStore.areReminderNotificationsEnabled(requireContext())
        ProfilePreferenceStore.setReminderNotificationsEnabled(requireContext(), enabled)

        if (enabled) {
            if (FittoNotificationPermission.requestIfNeeded(requireActivity())) {
                ReminderAlarmScheduler.scheduleAll(requireContext())
            }
        } else {
            ReminderAlarmScheduler.cancelAll(requireContext())
        }

        bindRows()
    }

    private fun toggleSocialNotifications() {
        val enabled = !ProfilePreferenceStore.areSocialNotificationsEnabled(requireContext())
        ProfilePreferenceStore.setSocialNotificationsEnabled(requireContext(), enabled)

        if (enabled) {
            FittoNotificationPermission.requestIfNeeded(requireActivity())
        }

        bindRows()
    }

    private fun toggleDirectMessageNotifications() {
        val enabled = !ProfilePreferenceStore.areDirectMessageNotificationsEnabled(requireContext())
        ProfilePreferenceStore.setDirectMessageNotificationsEnabled(requireContext(), enabled)

        if (enabled) {
            FittoNotificationPermission.requestIfNeeded(requireActivity())
        }

        bindRows()
    }

    private fun toggleGroupMessageNotifications() {
        val enabled = !ProfilePreferenceStore.areGroupMessageNotificationsEnabled(requireContext())
        ProfilePreferenceStore.setGroupMessageNotificationsEnabled(requireContext(), enabled)

        if (enabled) {
            FittoNotificationPermission.requestIfNeeded(requireActivity())
        }

        bindRows()
    }

    private fun toggleVibration() {
        val enabled = !ProfilePreferenceStore.isVibrationEnabled(requireContext())
        ProfilePreferenceStore.setVibrationEnabled(requireContext(), enabled)
        bindRows()
    }

    private fun showConfirmDialog(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes positiveResId: Int,
        iconResId: Int,
        iconColorResId: Int = R.color.profile_subtext,
        confirmTextColorResId: Int = R.color.profile_subtext,
        onConfirmed: () -> Unit
    ) {
        showFittoActionDialog(
            titleResId = titleResId,
            warningResId = null,
            messageResId = messageResId,
            positiveResId = positiveResId,
            negativeResId = R.string.profile_dialog_cancel,
            iconResId = iconResId,
            iconColorResId = iconColorResId,
            confirmTextColorResId = confirmTextColorResId,
            showPasswordInput = false,
            onConfirmed = { onConfirmed() }
        )
    }

    private fun showFittoActionDialog(
        @StringRes titleResId: Int,
        @StringRes warningResId: Int?,
        @StringRes messageResId: Int,
        @StringRes positiveResId: Int,
        @StringRes negativeResId: Int,
        iconResId: Int,
        iconColorResId: Int,
        confirmTextColorResId: Int = R.color.profile_subtext,
        cancelTextColorResId: Int = R.color.profile_subtext,
        showPasswordInput: Boolean,
        onConfirmed: (String?) -> Unit
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_fitto_action,
            null,
            false
        )

        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val passwordArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val passwordLabel = dialogView.findViewById<TextView>(R.id.textPasswordLabel)
        val passwordInputLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutPasswordInput)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.editActionPassword)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        titleText.setText(titleResId)
        messageText.setText(messageResId)
        cancelButton.setText(negativeResId)
        confirmButton.setText(positiveResId)

        warningText.isVisible = warningResId != null
        warningResId?.let { warningText.setText(it) }

        passwordArea.isVisible = showPasswordInput
        passwordLabel.setText(R.string.profile_delete_account_password_label)

        if (showPasswordInput) {
            passwordInputLayout.hint = getString(R.string.profile_delete_account_password_hint)
            configurePasswordInput(passwordInputLayout, passwordEditText)
            applySoftInputHintStyle(passwordInputLayout, passwordEditText)
        } else {
            passwordInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }

        iconImage.setImageResource(iconResId)
        iconImage.setColorFilter(requireContext().getColor(iconColorResId))
        cancelButton.setTextColor(requireContext().getColor(cancelTextColorResId))
        confirmButton.setTextColor(requireContext().getColor(confirmTextColorResId))

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            val password = passwordEditText.text?.toString().orEmpty()

            if (showPasswordInput && password.isBlank()) {
                passwordInputLayout.error = getString(R.string.profile_delete_account_empty_password)
                return@setOnClickListener
            }

            passwordInputLayout.error = null
            dialog.dismiss()
            onConfirmed(password.takeIf { showPasswordInput })
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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

    private fun configurePasswordInput(
        inputLayout: TextInputLayout,
        editText: EditText
    ) {
        inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

        editText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        editText.typeface = Typeface.DEFAULT
        editText.maxLines = 1
        editText.isSingleLine = true
    }

    private fun applySoftInputHintStyle(
        inputLayout: TextInputLayout,
        editText: EditText
    ) {
        val hintColor = colorWithAlpha(
            color = requireContext().getColor(R.color.profile_subtext),
            alpha = 0.58f
        )

        inputLayout.defaultHintTextColor = ColorStateList.valueOf(hintColor)
        editText.setHintTextColor(hintColor)
    }

    private fun colorWithAlpha(
        color: Int,
        alpha: Float
    ): Int {
        val boundedAlpha = alpha.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(color) * boundedAlpha).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun showDeleteAccountDialog() {
        showFittoActionDialog(
            titleResId = R.string.profile_detail_delete_account,
            warningResId = R.string.profile_delete_account_warning,
            messageResId = R.string.profile_delete_account_intro_message,
            positiveResId = R.string.profile_delete_account_continue,
            negativeResId = R.string.profile_delete_account_cancel,
            iconResId = R.drawable.ic_warning,
            iconColorResId = R.color.reminder_delete,
            confirmTextColorResId = R.color.profile_subtext,
            cancelTextColorResId = R.color.fitto_green,
            showPasswordInput = false,
            onConfirmed = {
                continueDeleteAccountAuthentication()
            }
        )
    }

    private fun continueDeleteAccountAuthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            navigateToWelcome()
            return
        }

        val providerIds = currentUser.providerData
            .map { providerInfo -> providerInfo.providerId }
            .filterNot { providerId -> providerId == FIREBASE_PROVIDER_ID }
            .toSet()

        when {
            providerIds.contains(EmailAuthProvider.PROVIDER_ID) -> showPasswordReauthDialog()
            providerIds.contains(GoogleAuthProvider.PROVIDER_ID) -> showGoogleReauthDialog()
            else -> showToast(R.string.profile_delete_account_provider_not_supported)
        }
    }

    private fun showPasswordReauthDialog() {
        showFittoActionDialog(
            titleResId = R.string.profile_delete_account_auth_title,
            warningResId = null,
            messageResId = R.string.profile_delete_account_password_message,
            positiveResId = R.string.profile_delete_account_auth_confirm,
            negativeResId = R.string.profile_delete_account_cancel,
            iconResId = R.drawable.ic_lock,
            iconColorResId = R.color.fitto_green,
            confirmTextColorResId = R.color.fitto_green,
            showPasswordInput = true,
            onConfirmed = { password ->
                reauthenticateWithPassword(password.orEmpty())
            }
        )
    }

    private fun reauthenticateWithPassword(password: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        if (currentUser == null || email.isNullOrBlank()) {
            showToast(R.string.profile_delete_account_error)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val credential = EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential).await()
                showFinalDeleteAccountConfirmDialog()
            } catch (exception: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "Account delete reauth failed: wrong password", exception)
                showToast(R.string.profile_delete_account_wrong_password)
            } catch (exception: Exception) {
                Log.e(TAG, "Account delete password reauth failed", exception)
                showToast(R.string.profile_delete_account_auth_error)
            }
        }
    }

    private fun showGoogleReauthDialog() {
        showFittoActionDialog(
            titleResId = R.string.profile_delete_account_auth_title,
            warningResId = null,
            messageResId = R.string.profile_delete_account_google_message,
            positiveResId = R.string.profile_delete_account_google_confirm,
            negativeResId = R.string.profile_delete_account_cancel,
            iconResId = R.drawable.ic_google_g,
            iconColorResId = R.color.fitto_green,
            confirmTextColorResId = R.color.fitto_green,
            showPasswordInput = false,
            onConfirmed = {
                reauthenticateWithGoogle()
            }
        )
    }

    private fun reauthenticateWithGoogle() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                GoogleSignInHelper.reauthenticateCurrentUser(requireContext())
                showFinalDeleteAccountConfirmDialog()
            } catch (exception: Exception) {
                Log.e(TAG, "Account delete Google reauth failed", exception)
                showToast(R.string.profile_delete_account_google_error)
            }
        }
    }

    private fun showFinalDeleteAccountConfirmDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_fitto_action,
            null,
            false
        )

        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val passwordArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        titleText.setText(R.string.profile_delete_account_final_title)
        messageText.setText(R.string.profile_delete_account_final_message)
        warningText.isVisible = false
        passwordArea.isVisible = false

        cancelButton.setText(R.string.profile_delete_account_cancel)
        confirmButton.setText(R.string.profile_delete_account_confirm)

        iconImage.setImageResource(R.drawable.ic_warning)
        iconImage.setColorFilter(requireContext().getColor(R.color.reminder_delete))
        cancelButton.setTextColor(requireContext().getColor(R.color.profile_subtext))
        confirmButton.setTextColor(requireContext().getColor(R.color.reminder_delete))

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            confirmButton.isEnabled = false
            cancelButton.isEnabled = false
            confirmButton.setText(R.string.profile_delete_account_deleting)
            performFinalAccountDelete(
                dialog = dialog,
                confirmButton = confirmButton,
                cancelButton = cancelButton
            )
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun performFinalAccountDelete(
        dialog: Dialog,
        confirmButton: MaterialButton,
        cancelButton: MaterialButton
    ) {
        val appContext = requireContext().applicationContext
        val deletedUserName = getString(R.string.deleted_user_display_name)
        val deletedMessageText = getString(R.string.deleted_message_text)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AccountDeletionRepository().deleteCurrentAccount(
                    deletedUserName = deletedUserName,
                    deletedMessageText = deletedMessageText
                )

                ProfilePreferenceStore.clearAccountScopedPreferences(appContext)
                ReminderPreferenceStore.clear(appContext)
                FittoNotificationStore.cancelAllActiveSystemNotifications(appContext)
                FittoNotificationStore.clearCurrentAccountNotifications(appContext)

                dialog.dismiss()
                showToast(R.string.profile_delete_account_success)
                navigateToWelcome()
            } catch (exception: FirebaseAuthRecentLoginRequiredException) {
                Log.e(TAG, "Firebase Auth account delete requires recent login", exception)
                confirmButton.isEnabled = true
                cancelButton.isEnabled = true
                confirmButton.setText(R.string.profile_delete_account_confirm)
                showToast(R.string.profile_delete_account_requires_recent_login)
            } catch (exception: Exception) {
                Log.e(TAG, "Firebase Auth account delete failed", exception)
                confirmButton.isEnabled = true
                cancelButton.isEnabled = true
                confirmButton.setText(R.string.profile_delete_account_confirm)
                showToast(R.string.profile_delete_account_error)
            }
        }
    }

    private fun performLogout() {
        val appContext = requireContext().applicationContext
        FittoNotificationStore.cancelAllActiveSystemNotifications(appContext)
        FittoNotificationStore.clearCurrentAccountNotifications(appContext)
        FirebaseAuth.getInstance().signOut()
        navigateToWelcome()
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

    private fun showCountryPickerSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetCountryPickerBinding.inflate(layoutInflater)

        val countries = createCountryList()
        var temporarySelectedCountry = countries.firstOrNull {
            it.name == ProfilePreferenceStore.getRegion(requireContext())
        } ?: countries.firstOrNull { it.code == TURKEY_CODE }
        ?: countries.first()

        val countryAdapter = CountryAdapter(
            selectedCountryCode = temporarySelectedCountry.code,
            onCountryClick = { country ->
                temporarySelectedCountry = country
            }
        )

        sheetBinding.countryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = countryAdapter
            setHasFixedSize(false)
        }

        fun submitCountries(query: String = "") {
            val cleanQuery = query.trim()

            val filteredCountries = if (cleanQuery.isBlank()) {
                countries
            } else {
                countries.filter { country ->
                    country.name.contains(cleanQuery, ignoreCase = true) ||
                            country.code.contains(cleanQuery, ignoreCase = true)
                }
            }

            countryAdapter.submitList(filteredCountries)
        }

        sheetBinding.countrySearchEditText.doAfterTextChanged { editable ->
            submitCountries(editable?.toString().orEmpty())
        }

        sheetBinding.countryPickerDoneText.setOnClickListener {
            ProfilePreferenceStore.setRegion(
                context = requireContext(),
                region = temporarySelectedCountry.name
            )
            bindRows()
            dialog.dismiss()
        }

        submitCountries()

        dialog.setContentView(sheetBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = sheetBinding.root.parent as? View

            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        dialog.show()
    }

    private fun createCountryList(): List<CountryUiModel> {
        val displayLocale = when (ProfilePreferenceStore.getLanguage(requireContext())) {
            ProfileLanguage.TURKISH -> Locale("tr", "TR")
            ProfileLanguage.ENGLISH -> Locale.ENGLISH
        }

        return Locale.getISOCountries()
            .mapNotNull { code ->
                val countryLocale = Locale("", code)
                val countryName = countryLocale.getDisplayCountry(displayLocale)

                if (countryName.isBlank()) {
                    null
                } else {
                    CountryUiModel(
                        code = code,
                        name = countryName,
                        flagEmoji = code.toFlagEmoji(),
                        isRecommended = code == TURKEY_CODE
                    )
                }
            }
            .sortedWith(
                compareBy<CountryUiModel> {
                    if (it.code == TURKEY_CODE) 0 else 1
                }.thenBy {
                    it.name.lowercase(displayLocale)
                }
            )
    }

    private fun String.toFlagEmoji(): String {
        val countryCode = uppercase(Locale.US)

        if (countryCode.length != 2) return "🌍"

        val firstLetter =
            Character.codePointAt(countryCode, 0) - 'A'.code + REGIONAL_INDICATOR_OFFSET

        val secondLetter =
            Character.codePointAt(countryCode, 1) - 'A'.code + REGIONAL_INDICATOR_OFFSET

        return String(Character.toChars(firstLetter)) +
                String(Character.toChars(secondLetter))
    }

    private data class CountryUiModel(
        val code: String,
        val name: String,
        val flagEmoji: String,
        val isRecommended: Boolean = false
    )

    private class CountryAdapter(
        selectedCountryCode: String,
        private val onCountryClick: (CountryUiModel) -> Unit
    ) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

        private var countries: List<CountryUiModel> = emptyList()
        private var selectedCountryCode: String = selectedCountryCode

        fun submitList(newCountries: List<CountryUiModel>) {
            countries = newCountries
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CountryViewHolder {
            val binding = ItemCountryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            return CountryViewHolder(binding)
        }

        override fun getItemCount(): Int = countries.size

        override fun onBindViewHolder(
            holder: CountryViewHolder,
            position: Int
        ) {
            holder.bind(countries[position])
        }

        inner class CountryViewHolder(
            private val binding: ItemCountryBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(country: CountryUiModel) = with(binding) {
                val isSelected = selectedCountryCode == country.code

                countryFlagText.text = country.flagEmoji
                countryNameText.text = country.name

                countryCheckContainer.isVisible = isSelected
                countryChevronIcon.isVisible = !isSelected

                root.setOnClickListener {
                    selectedCountryCode = country.code
                    onCountryClick(country)
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun isAchievementMenu(menuId: ProfileMenuId): Boolean {
        return menuId == ProfileMenuId.ACHIEVEMENTS ||
                menuId == ProfileMenuId.ACHIEVEMENT_BADGES ||
                menuId == ProfileMenuId.ACHIEVEMENT_STREAK ||
                menuId == ProfileMenuId.ACHIEVEMENT_WATER
    }

    companion object {
        private const val ARG_MENU_ID = "menu_id"
        private const val TURKEY_CODE = "TR"
        private const val REGIONAL_INDICATOR_OFFSET = 0x1F1E6
        private const val FIREBASE_PROVIDER_ID = "firebase"
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

    private data class ChoiceOption<T>(
        val title: String,
        val value: T,
        val isSelected: Boolean
    )

    private fun <T> showFittoChoiceDialog(
        @StringRes titleResId: Int,
        options: List<ChoiceOption<T>>,
        onSelected: (T) -> Unit
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_profile_choice,
            null,
            false
        )

        val titleTextView = dialogView.findViewById<TextView>(R.id.textChoiceDialogTitle)
        val optionsLayout = dialogView.findViewById<LinearLayout>(R.id.layoutChoiceOptions)
        val cancelButton = dialogView.findViewById<TextView>(R.id.buttonChoiceDialogCancel)

        titleTextView.setText(titleResId)
        optionsLayout.removeAllViews()

        options.forEach { option ->
            val optionView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_profile_choice, optionsLayout, false)

            val optionTitle = optionView.findViewById<TextView>(R.id.textChoiceTitle)
            val checkText = optionView.findViewById<TextView>(R.id.textChoiceCheck)

            optionTitle.text = option.title
            checkText.visibility = if (option.isSelected) View.VISIBLE else View.INVISIBLE

            optionView.setOnClickListener {
                onSelected(option.value)
                dialog.dismiss()
            }

            optionsLayout.addView(optionView)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}