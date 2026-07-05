package com.example.fitto.feature_home.presentation.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPermission
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
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
    private val profileDependencies by lazy {
        requireContext().fittoAppContainer.profileFeatureDependencies
    }
    private val socialRepository by lazy { profileDependencies.socialRepository }
    private val featureScope: ProfileDetailFeatureScope by lazy {
        ProfileDetailFeatureScopeResolver.resolve(menuId)
    }
    private var coordinators: ProfileDetailCoordinatorSet? = null
    private var actionCoordinator: ProfileDetailActionCoordinator? = null
    private var photoAlbumCoordinator: ProfilePhotoAlbumCoordinator? = null
    private var selectedPhotoAlbumDateKey: String = ProfilePhotoAlbumFilter.ALL
    private var pendingPhotoDocumentItem: ProfilePhotoAlbumItem? = null

    private var detailListRenderer: ProfileDetailListRenderer? = null

    private var lastProfileErrorMessage: String? = null

    private val notificationSoundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        coordinators?.notificationSettings?.saveCustomNotificationSound(uri)
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

        viewModel = obtainProfileViewModel()
        setupCoordinators()

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
            onItemClick = { item -> actionCoordinator?.handle(item) },
            onAchievementTarget = ::openNestedProfileDetail
        )
        if (featureScope.usesPhotoAlbum) {
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
        if (!featureScope.usesPhotoAlbum) {
            bindRows()
        }
        viewModel.loadProfile()

        if (featureScope.loadsBlockedUsers) {
            coordinators?.dataManagement?.loadBlockedUsers()
        }
    }

    override fun onResume() {
        super.onResume()

        when (featureScope.resumeEffect) {
            ProfileDetailResumeEffect.REFRESH_ACCOUNT_EMAIL ->
                coordinators?.accountEdit?.refreshAccountEmail()
            ProfileDetailResumeEffect.REFRESH_ROWS -> bindRows()
            ProfileDetailResumeEffect.NONE -> Unit
        }
    }

    override fun onDestroyView() {
        actionCoordinator = null
        coordinators?.clear()
        coordinators = null
        photoAlbumCoordinator?.clear()
        photoAlbumCoordinator = null
        detailListRenderer?.clear()
        detailListRenderer = null
        super.onDestroyView()
    }

    private fun setupCoordinators() {
        val coordinatorSet = ProfileDetailCoordinatorFactory(
            context = requireContext(),
            inflater = layoutInflater,
            fragmentManager = childFragmentManager,
            scope = viewLifecycleOwner.lifecycleScope,
            menuId = menuId,
            callbacks = ProfileDetailCoordinatorCallbacks(
                refreshRows = ::bindRows,
                refreshProfile = viewModel::refreshProfile,
                currentUser = { latestUser },
                updateCurrentUser = { user -> latestUser = user },
                updateBlockedUsers = { users -> latestBlockedUsers = users },
                navigateToWelcome = ::navigateToWelcome,
                emailLanguageCode = ::emailLanguageCode,
                requestNotificationPermission = {
                    FittoNotificationPermission.requestIfNeeded(requireActivity())
                },
                openNotificationSoundPicker = {
                    notificationSoundPickerLauncher.launch(arrayOf("audio/*"))
                },
                launchIntent = ::startActivity,
                showMessage = ::showToast
            ),
            accountSettingsRepository = profileDependencies.accountSettingsRepository,
            socialRepository = socialRepository
        ).create()

        coordinators = coordinatorSet
        actionCoordinator = ProfileDetailActionCoordinator(
            coordinators = coordinatorSet,
            currentUser = { latestUser },
            currentBlockedUsers = { latestBlockedUsers },
            exportRequest = ::profileDataExportRequest,
            openNestedProfileDetail = ::openNestedProfileDetail,
            openHealthConnect = {
                HealthConnectBottomSheetFragment()
                    .show(childFragmentManager, HEALTH_CONNECT_DIALOG_TAG)
            },
            toggleComments = ::toggleComments,
            showMessage = ::showToast
        )
    }

    private fun accountDisplayUser(): SocialUser? {
        return ProfileAccountDisplayResolver.resolve(
            user = latestUser,
            authEmail = coordinators?.currentAuthEmail?.invoke().orEmpty()
        )
    }

    private fun observeProfileData() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            latestHealthData = state.healthData
            latestUser = state.user ?: latestUser
            latestUser = accountDisplayUser()

            if (featureScope.usesPhotoAlbum) {
                photoAlbumCoordinator?.updateProfilePhoto(latestUser?.photoUrl)
            } else {
                bindRows()
            }

            val errorMessage = state.errorMessage
            val shouldSuppressSessionError = menuId == ProfileMenuId.ACCOUNT_MANAGEMENT &&
                    coordinators?.emailChangeDialogs?.isRecoveryActive == true &&
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

    private fun profileDataExportRequest(): ProfileDataExportRequest {
        val state = viewModel.uiState.value
        return ProfileDataExportRequest(
            user = latestUser,
            healthData = latestHealthData,
            bodyMeasurements = state?.bodyMeasurements ?: BodyMeasurementsData(),
            bodyMeasurementHistory = state?.bodyMeasurementsHistory.orEmpty()
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

    private fun emailLanguageCode(): String {
        return when (ProfilePreferenceStore.getLanguage(requireContext())) {
            ProfileLanguage.TURKISH -> "tr"
            ProfileLanguage.ENGLISH -> "en"
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
        private const val HEALTH_CONNECT_DIALOG_TAG = "profile_health_connect"

        fun newInstance(menuId: ProfileMenuId): ProfileDetailFragment {
            return ProfileDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MENU_ID, menuId.name)
                }
            }
        }
    }

}
