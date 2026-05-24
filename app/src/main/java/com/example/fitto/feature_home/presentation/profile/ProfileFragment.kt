package com.example.fitto.feature_home.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationBadgeBinder
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileRemindersActivity
import com.example.fitto.feature_home.presentation.social.NotificationsFragment
import com.example.fitto.feature_home.presentation.social.SocialProfileFragment
import com.example.fitto.feature_home.presentation.social.SocialUser
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.session.UserSessionDataGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
interface ProfileScreenNavigator {
    fun openProfileWeightScreen()
    fun openProfileBodyMeasurementsScreen()
    fun openProfileDetailScreen(menuId: ProfileMenuId)
    fun openReportsScreen()
}

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var viewModel: ProfileViewModel

    private var latestHealthData: ProfileHealthData = ProfileHealthData.Empty
    private var latestUser: SocialUser? = null
    private var latestPostCount: Int = 0
    private var latestWeeklySummary: ProfileWeeklySummary = ProfileWeeklySummary.Empty
    private var lastScrollY: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lastScrollY = savedInstanceState?.getInt(KEY_SCROLL_Y) ?: lastScrollY
        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]

        bindProfile(
            root = view,
            user = latestUser,
            postCount = latestPostCount,
            healthData = latestHealthData
        )

        view.findViewById<android.widget.TextView>(R.id.profileNotificationBadgeText)?.let { badge ->
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
            latestUser = state.user ?: latestUser
            latestPostCount = state.postCount
            latestHealthData = state.healthData

            val currentScrollY = root.findViewById<NestedScrollView>(R.id.profileScrollView)?.scrollY ?: lastScrollY

            bindProfile(
                root = root,
                user = latestUser,
                postCount = latestPostCount,
                healthData = latestHealthData
            )

            lastScrollY = currentScrollY
            restoreScrollPosition(root)

            state.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindProfile(
        root: View,
        user: SocialUser?,
        postCount: Int,
        healthData: ProfileHealthData
    ) {
        ProfileBinder.bind(
            root = root,
            user = user,
            postCount = postCount,
            healthData = healthData,
            weeklySummary = latestWeeklySummary,
            onProfileImageClick = {
                openMySocialProfile()
            },
            onNotificationClick = {
                openNotifications()
            },
            onWeeklySummaryClick = {
                rememberScrollPosition()
                profileScreenNavigator()?.openReportsScreen()
            },
            onGoalsClick = {
                rememberScrollPosition()
                profileScreenNavigator()?.openProfileDetailScreen(ProfileMenuId.DAILY_GOALS)
            },
            onMenuItemClick = { menuId ->
                handleMenuClick(menuId)
            }
        )
    }

    private fun loadWeeklySummary() {
        val context = context ?: return
        val database = FittoDatabaseProvider.getDatabase(context.applicationContext)

        viewLifecycleOwner.lifecycleScope.launch {
            val summary = withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val dates = (6 downTo 0).map { offset ->
                    today.minusDays(offset.toLong())
                }

                val summaries = dates.map { date ->
                    database.mealDao().getNutritionSummaryForDate(UserSessionDataGuard.getActiveUid(context.applicationContext) ?: "local_anonymous", date.toString())
                }

                val todaySummary = summaries.lastOrNull()
                val recordedDays = summaries.count { item ->
                    item.totalCalories > 0 ||
                            item.totalProtein > 0.0 ||
                            item.totalCarbs > 0.0 ||
                            item.totalFat > 0.0
                }

                val weeklyCaloriesTotal = summaries.sumOf { item ->
                    item.totalCalories
                }

                ProfileWeeklySummary(
                    todayCalories = todaySummary?.totalCalories ?: 0,
                    todayProtein = todaySummary?.totalProtein ?: 0.0,
                    todayCarbs = todaySummary?.totalCarbs ?: 0.0,
                    todayFat = todaySummary?.totalFat ?: 0.0,
                    weeklyTotalCalories = weeklyCaloriesTotal,
                    weeklyAverageCalories = if (recordedDays > 0) {
                        weeklyCaloriesTotal / recordedDays
                    } else {
                        0
                    },
                    recordedDays = recordedDays,
                    weeklyProtein = summaries.sumOf { item -> item.totalProtein },
                    weeklyCarbs = summaries.sumOf { item -> item.totalCarbs },
                    weeklyFat = summaries.sumOf { item -> item.totalFat }
                )
            }

            if (!isAdded) return@launch

            latestWeeklySummary = summary

            view?.let { root ->
                bindProfile(
                    root = root,
                    user = latestUser,
                    postCount = latestPostCount,
                    healthData = latestHealthData
                )
            }
        }
    }
    fun scrollToTopFromBottomTab() {
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
                startActivity(
                    Intent(requireContext(), ProfileRemindersActivity::class.java)
                )
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid

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
        if (lastScrollY <= 0) return

        root.findViewById<NestedScrollView>(R.id.profileScrollView)?.post {
            root.findViewById<NestedScrollView>(R.id.profileScrollView)
                ?.scrollTo(0, lastScrollY)
        }
    }

    private fun rememberScrollPosition() {
        val scrollView = view?.findViewById<NestedScrollView>(R.id.profileScrollView)
        lastScrollY = scrollView?.scrollY ?: lastScrollY
    }

    override fun onPause() {
        rememberScrollPosition()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rememberScrollPosition()
        outState.putInt(KEY_SCROLL_Y, lastScrollY)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_SCROLL_Y = "profile_scroll_y"

        private const val PROFILE_UPDATED_RESULT_KEY = "social_profile_updated"
    }
}