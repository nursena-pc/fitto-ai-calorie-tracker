package com.example.fitto.feature_home.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationBadgeBinder
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileRemindersActivity
import com.example.fitto.feature_home.presentation.social.NotificationsFragment
import com.example.fitto.feature_home.presentation.social.SocialProfileFragment
import kotlinx.coroutines.launch

interface ProfileScreenNavigator {
    fun openProfileWeightScreen()
    fun openProfileBodyMeasurementsScreen()
    fun openProfileDetailScreen(menuId: ProfileMenuId)
    fun openReportsScreen()
}

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var viewModel: ProfileViewModel
    private val screenStateStore = ProfileScreenStateStore()
    private val profileDependencies by lazy {
        requireContext().fittoAppContainer.profileFeatureDependencies
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.getInt(KEY_SCROLL_Y)
            ?.let(screenStateStore::updateScrollY)
        viewModel = obtainProfileViewModel()

        renderProfile(view)

        view.findViewById<TextView>(R.id.profileNotificationBadgeText)?.let { badge ->
            FittoNotificationBadgeBinder.bind(requireContext(), viewLifecycleOwner, badge)
        }

        observeViewModel(view)
        observeSocialProfileUpdates()
        viewModel.loadProfile()
        loadWeeklySummary()
        restoreScrollPosition(view)
    }

    override fun onResume() {
        super.onResume()

        if (::viewModel.isInitialized) {
            viewModel.refreshProfile()
        }
        loadWeeklySummary()
    }

    private fun observeSocialProfileUpdates() {
        parentFragmentManager.setFragmentResultListener(
            PROFILE_UPDATED_RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.refreshProfile()
        }
    }

    private fun observeViewModel(root: View) {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            rememberScrollPosition(root)
            screenStateStore.applyProfile(state)
            renderProfile(root)
            restoreScrollPosition(root)

            state.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderProfile(root: View) {
        val state = screenStateStore.state
        ProfileBinder.bind(
            root = root,
            user = state.user,
            postCount = state.postCount,
            healthData = state.healthData,
            weeklySummary = state.weeklySummary,
            onProfileImageClick = ::openMySocialProfile,
            onNotificationClick = ::openNotifications,
            onWeeklySummaryClick = {
                rememberScrollPosition()
                profileScreenNavigator()?.openReportsScreen()
            },
            onGoalsClick = {
                rememberScrollPosition()
                profileScreenNavigator()?.openProfileDetailScreen(ProfileMenuId.DAILY_GOALS)
            },
            onMenuItemClick = ::handleMenuClick
        )
    }

    private fun loadWeeklySummary() {
        if (!isAdded || view == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            val summary = profileDependencies.weeklySummaryLoader.load()
            if (!isAdded) return@launch

            screenStateStore.updateWeeklySummary(summary)
            view?.let(::renderProfile)
        }
    }

    fun scrollToTopFromBottomTab() {
        screenStateStore.updateScrollY(0)
        view?.findViewById<NestedScrollView>(R.id.profileScrollView)
            ?.post {
                view?.findViewById<NestedScrollView>(R.id.profileScrollView)
                    ?.scrollTo(0, 0)
            }
    }

    fun refreshProfileFromHome() {
        if (::viewModel.isInitialized) {
            viewModel.refreshProfile()
        }
    }

    private fun handleMenuClick(menuId: ProfileMenuId) {
        when (menuId) {
            ProfileMenuId.WEIGHT -> {
                rememberScrollPosition()
                profileScreenNavigator()?.openProfileWeightScreen()
            }

            ProfileMenuId.BODY_MEASUREMENTS -> {
                rememberScrollPosition()
                profileScreenNavigator()?.openProfileBodyMeasurementsScreen()
            }

            ProfileMenuId.REMINDERS -> {
                startActivity(Intent(requireContext(), ProfileRemindersActivity::class.java))
            }

            else -> {
                rememberScrollPosition()
                profileScreenNavigator()?.openProfileDetailScreen(menuId)
            }
        }
    }

    private fun profileScreenNavigator(): ProfileScreenNavigator? {
        return parentFragment as? ProfileScreenNavigator
    }

    private fun openNotifications() {
        NotificationsFragment()
            .show(childFragmentManager, "NotificationsFragment")
    }

    private fun openMySocialProfile() {
        val uid = profileDependencies.currentUserId()

        if (uid.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.profile_data_not_found),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        SocialProfileFragment
            .newInstance(uid)
            .show(parentFragmentManager, "my_social_profile")
    }

    private fun restoreScrollPosition(root: View) {
        val scrollY = screenStateStore.state.scrollY
        if (scrollY <= 0) return

        root.findViewById<NestedScrollView>(R.id.profileScrollView)?.post {
            root.findViewById<NestedScrollView>(R.id.profileScrollView)
                ?.scrollTo(0, scrollY)
        }
    }

    private fun rememberScrollPosition(root: View? = view) {
        val scrollView = root?.findViewById<NestedScrollView>(R.id.profileScrollView)
        screenStateStore.updateScrollY(scrollView?.scrollY ?: screenStateStore.state.scrollY)
    }

    override fun onPause() {
        rememberScrollPosition()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rememberScrollPosition()
        outState.putInt(KEY_SCROLL_Y, screenStateStore.state.scrollY)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_SCROLL_Y = "profile_scroll_y"
        private const val PROFILE_UPDATED_RESULT_KEY = "social_profile_updated"
    }
}
