package com.example.fitto.feature_home.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.FragmentHomeBinding
import com.example.fitto.feature_daily.DailyFragment
import com.example.fitto.feature_home.presentation.profile.PendingHomeRestore
import com.example.fitto.feature_home.presentation.profile.ProfileBodyMeasurementsFragment
import com.example.fitto.feature_home.presentation.profile.ProfileDetailFragment
import com.example.fitto.feature_home.presentation.profile.ProfileFragment
import com.example.fitto.feature_home.presentation.profile.ProfileMenuId
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_home.presentation.profile.ProfileScreenNavigator
import com.example.fitto.feature_home.presentation.profile.ProfileWeightFragment
import com.example.fitto.feature_home.presentation.navigation.HomeNavigationRequest
import com.example.fitto.feature_home.presentation.navigation.HomeNotificationRouteResolver
import com.example.fitto.feature_home.presentation.social.SocialFragment
import com.example.fitto.feature_reports.ReportsFragment
import com.example.fitto.feature_recipes.RecipesFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home), ProfileScreenNavigator {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileCacheRepository: ProfileCacheRepository

    private var activeTabId: Int = R.id.nav_daily

    private val profileBackStackListener =
        FragmentManager.OnBackStackChangedListener {
            updateBottomBarVisibility()
            (currentVisibleHomeFragment() as? ProfileFragment)?.refreshProfileFromHome()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)
        profileCacheRepository = ProfileCacheRepository(requireContext().applicationContext)

        syncProfileCacheFromFirebase()

        childFragmentManager.addOnBackStackChangedListener(profileBackStackListener)

        val restore = ProfilePreferenceStore.consumePendingHomeRestore(requireContext())

        activeTabId = restore?.tabId?.takeIf { it != 0 }
            ?: savedInstanceState?.getInt(KEY_ACTIVE_TAB_ID)
                    ?: R.id.nav_daily

        binding.bottomNavigation.menu.findItem(activeTabId)?.isChecked = true

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == activeTabId) {
                return@setOnItemSelectedListener true
            }

            val previousTabId = activeTabId
            activeTabId = item.itemId

            showTab(
                tabId = activeTabId,
                previousTabId = previousTabId
            )

            true
        }
        updateBottomBarVisibility()

        binding.bottomNavigation.setOnItemReselectedListener {
            // Aynı sekmeye tekrar basınca fragment yeniden kurulmasın.
        }

        showTab(
            tabId = activeTabId,
            previousTabId = null
        )

        restoreProfileScreenIfNeeded(restore)
        updateBottomBarVisibility()
    }


    private fun syncProfileCacheFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    profileCacheRepository.syncCurrentUserProfileFromFirebase()
                }
            }.onSuccess { profile ->
                Log.d(
                    TAG,
                    "Profile cache sync completed. dailyTarget=${profile?.dailyCalorieTarget}"
                )
            }.onFailure { exception ->
                Log.e(TAG, "Profile cache sync failed", exception)
            }
        }
    }

    fun onAccountScopeChanged() {
        syncProfileCacheFromFirebase()
        (currentVisibleHomeFragment() as? ProfileFragment)?.refreshProfileFromHome()
    }

    override fun openProfileWeightScreen() {
        rememberProfileTabAsActive()
        ensureProfileRootExistsForNestedNavigation()

        val currentFragment = currentVisibleHomeFragment()
        if (currentFragment is ProfileWeightFragment) {
            updateBottomBarVisibility()
            return
        }

        showNestedProfileScreen(
            fragment = ProfileWeightFragment(),
            tag = TAG_PROFILE_WEIGHT,
            backStackName = BACK_STACK_PROFILE_WEIGHT
        )
    }

    override fun openProfileBodyMeasurementsScreen() {
        rememberProfileTabAsActive()
        ensureProfileRootExistsForNestedNavigation()

        val currentFragment = currentVisibleHomeFragment()
        if (currentFragment is ProfileBodyMeasurementsFragment) {
            updateBottomBarVisibility()
            return
        }

        showNestedProfileScreen(
            fragment = ProfileBodyMeasurementsFragment(),
            tag = TAG_PROFILE_BODY_MEASUREMENTS,
            backStackName = BACK_STACK_PROFILE_BODY_MEASUREMENTS
        )
    }

    override fun openProfileDetailScreen(menuId: ProfileMenuId) {
        rememberProfileTabAsActive()
        ensureProfileRootExistsForNestedNavigation()

        val currentFragment = currentVisibleHomeFragment()
        if (currentFragment is ProfileDetailFragment && currentFragment.currentMenuId() == menuId) {
            updateBottomBarVisibility()
            return
        }

        showNestedProfileScreen(
            fragment = ProfileDetailFragment.newInstance(menuId),
            tag = TAG_PROFILE_DETAIL_PREFIX + menuId.name,
            backStackName = BACK_STACK_PROFILE_DETAIL
        )
    }


    internal fun navigate(request: HomeNavigationRequest) {
        when (val destination = HomeNotificationRouteResolver.resolve(request)) {
            is HomeNotificationRouteResolver.Destination.Social -> {
                openSocialNotificationTarget(
                    type = destination.type,
                    targetId = destination.targetId,
                    secondaryTargetId = destination.secondaryTargetId
                )
            }

            HomeNotificationRouteResolver.Destination.ProfileWeight -> {
                openProfileWeightScreen()
            }

            is HomeNotificationRouteResolver.Destination.DailyReminder -> {
                openDailyReminderTarget(
                    categoryName = destination.categoryName,
                    reminderId = destination.reminderId
                )
            }
        }
    }

    private fun openSocialNotificationTarget(
        type: String?,
        targetId: String?,
        secondaryTargetId: String?
    ) {
        activeTabId = R.id.nav_social
        binding.bottomNavigation.menu.findItem(R.id.nav_social)?.isChecked = true

        val socialFragment = showHomeFragment(TAG_SOCIAL) {
            SocialFragment()
        } as? SocialFragment

        updateBottomBarVisibility()

        binding.root.post {
            val targetFragment = socialFragment
                ?: childFragmentManager.findFragmentByTag(TAG_SOCIAL) as? SocialFragment

            targetFragment?.openNotificationTarget(
                type = type,
                targetId = targetId,
                secondaryTargetId = secondaryTargetId
            )
        }
    }

    private fun openDailyReminderTarget(
        categoryName: String?,
        reminderId: String?
    ) {
        activeTabId = R.id.nav_daily
        binding.bottomNavigation.menu.findItem(R.id.nav_daily)?.isChecked = true

        val dailyFragment = showHomeFragment(TAG_DAILY) {
            DailyFragment()
        } as? DailyFragment

        updateBottomBarVisibility()

        binding.root.post {
            val targetFragment = dailyFragment
                ?: childFragmentManager.findFragmentByTag(TAG_DAILY) as? DailyFragment

            targetFragment?.openReminderTarget(
                categoryName = categoryName,
                reminderId = reminderId
            )
        }
    }

    fun rememberCurrentScreenForSettingsRecreate() {
        val profileScreen = when (val fragment = currentVisibleHomeFragment()) {
            is ProfileDetailFragment -> {
                PROFILE_SCREEN_DETAIL_PREFIX + fragment.currentMenuId().name
            }

            is ProfileWeightFragment -> {
                PROFILE_SCREEN_WEIGHT
            }

            is ProfileBodyMeasurementsFragment -> {
                PROFILE_SCREEN_BODY_MEASUREMENTS
            }

            is ProfileFragment -> {
                PROFILE_SCREEN_MAIN
            }

            else -> null
        }

        ProfilePreferenceStore.rememberPendingHomeRestore(
            context = requireContext(),
            tabId = activeTabId,
            profileScreen = profileScreen
        )
    }

    private fun showTab(
        tabId: Int,
        previousTabId: Int?
    ) {
        if (previousTabId == R.id.nav_profile && tabId != R.id.nav_profile) {
            clearProfileBackStack()
        }

        when (tabId) {
            R.id.nav_social -> {
                showHomeFragment(TAG_SOCIAL) {
                    SocialFragment()
                }
            }

            R.id.nav_recipes -> {
                showHomeFragment(TAG_RECIPES) {
                    RecipesFragment()
                }
            }

            R.id.nav_daily -> {
                showHomeFragment(TAG_DAILY) {
                    DailyFragment()
                }
            }

            R.id.nav_reports -> {
                showHomeFragment(TAG_REPORTS) {
                    ReportsFragment()
                }
            }

            R.id.nav_profile -> {
                val profileFragment = showHomeFragment(TAG_PROFILE) {
                    ProfileFragment()
                } as? ProfileFragment

                val cameFromAnotherTab =
                    previousTabId != null && previousTabId != R.id.nav_profile

                if (cameFromAnotherTab) {
                    profileFragment?.scrollToTopFromBottomTab()
                }

                profileFragment?.refreshProfileFromHome()
            }
        }

        updateBottomBarVisibility()
    }

    private fun showHomeFragment(
        tag: String,
        fragmentFactory: () -> Fragment
    ): Fragment? {
        removePlaceholderViews()

        val fragmentManager = childFragmentManager

        if (fragmentManager.isStateSaved) {
            return null
        }

        val existingFragment = fragmentManager.findFragmentByTag(tag)
        val targetFragment = existingFragment ?: fragmentFactory()

        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                fragmentManager.fragments
                    .filter { fragment ->
                        fragment.id == R.id.homeTabContainer
                    }
                    .forEach { fragment ->
                        hide(fragment)
                    }

                if (existingFragment == null) {
                    add(
                        R.id.homeTabContainer,
                        targetFragment,
                        tag
                    )
                } else {
                    show(targetFragment)
                }
            }
            .commitNowAllowingStateLoss()

        return targetFragment
    }

    private fun restoreProfileScreenIfNeeded(restore: PendingHomeRestore?) {
        if (restore?.tabId != R.id.nav_profile) {
            return
        }

        val screen = restore.profileScreen.orEmpty()

        if (screen.isBlank() || screen == PROFILE_SCREEN_MAIN) {
            return
        }

        clearProfileBackStack()
        rememberProfileTabAsActive()
        ensureProfileRootExistsForNestedNavigation()

        when {
            screen == PROFILE_SCREEN_WEIGHT -> {
                showNestedProfileScreen(
                    fragment = ProfileWeightFragment(),
                    tag = TAG_PROFILE_WEIGHT,
                    backStackName = BACK_STACK_PROFILE_WEIGHT
                )
            }

            screen == PROFILE_SCREEN_BODY_MEASUREMENTS -> {
                showNestedProfileScreen(
                    fragment = ProfileBodyMeasurementsFragment(),
                    tag = TAG_PROFILE_BODY_MEASUREMENTS,
                    backStackName = BACK_STACK_PROFILE_BODY_MEASUREMENTS
                )
            }

            screen.startsWith(PROFILE_SCREEN_DETAIL_PREFIX) -> {
                val menuId = screen
                    .removePrefix(PROFILE_SCREEN_DETAIL_PREFIX)
                    .let { rawValue ->
                        runCatching {
                            ProfileMenuId.valueOf(rawValue)
                        }.getOrDefault(ProfileMenuId.SETTINGS)
                    }

                showNestedProfileScreen(
                    fragment = ProfileDetailFragment.newInstance(menuId),
                    tag = TAG_PROFILE_DETAIL_PREFIX + menuId.name,
                    backStackName = BACK_STACK_PROFILE_DETAIL
                )
            }
        }
    }

    private fun showNestedProfileScreen(
        fragment: Fragment,
        tag: String,
        backStackName: String
    ) {
        binding.bottomNavigation.visibility = View.GONE
        removePlaceholderViews()

        val fragmentManager = childFragmentManager

        if (fragmentManager.isStateSaved) {
            return
        }

        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                fragmentManager.fragments
                    .filter { existing ->
                        existing.id == R.id.homeTabContainer && !existing.isHidden
                    }
                    .forEach { visible ->
                        hide(visible)
                    }

                add(
                    R.id.homeTabContainer,
                    fragment,
                    tag
                )

                addToBackStack(backStackName)
            }
            .commitAllowingStateLoss()

        fragmentManager.executePendingTransactions()
        updateBottomBarVisibility()
    }

    private fun rememberProfileTabAsActive() {
        activeTabId = R.id.nav_profile
        binding.bottomNavigation.menu.findItem(R.id.nav_profile)?.isChecked = true
    }

    private fun ensureProfileRootExistsForNestedNavigation() {
        val currentFragment = currentVisibleHomeFragment()
        val isAlreadyInsideProfileFlow = currentFragment is ProfileFragment ||
                currentFragment is ProfileDetailFragment ||
                currentFragment is ProfileWeightFragment ||
                currentFragment is ProfileBodyMeasurementsFragment

        val hasProfileRoot = childFragmentManager.findFragmentByTag(TAG_PROFILE) != null

        if (!hasProfileRoot || !isAlreadyInsideProfileFlow) {
            showHomeFragment(TAG_PROFILE) {
                ProfileFragment()
            }
        }
    }

    private fun currentVisibleHomeFragment(): Fragment? {
        return childFragmentManager.fragments.lastOrNull { fragment ->
            fragment.id == R.id.homeTabContainer &&
                    fragment.isVisible &&
                    !fragment.isHidden
        }
    }

    private fun hideVisibleHomeFragments() {
        val visibleFragments = childFragmentManager.fragments.filter { fragment ->
            fragment.id == R.id.homeTabContainer && !fragment.isHidden
        }

        if (visibleFragments.isEmpty()) {
            return
        }

        childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                visibleFragments.forEach { fragment ->
                    hide(fragment)
                }
            }
            .commitNowAllowingStateLoss()
    }

    private fun clearProfileBackStack() {
        if (childFragmentManager.backStackEntryCount == 0) {
            return
        }

        childFragmentManager.popBackStackImmediate(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    private fun updateBottomBarVisibility() {
        binding.bottomNavigation.visibility =
            if (HomeBottomBarVisibilityPolicy.shouldHideFor(currentVisibleHomeFragment())) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun removePlaceholderViews() {
        for (index in binding.homeTabContainer.childCount - 1 downTo 0) {
            val child = binding.homeTabContainer.getChildAt(index)

            if (child.tag == TAG_PLACEHOLDER_VIEW) {
                binding.homeTabContainer.removeViewAt(index)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_ACTIVE_TAB_ID, activeTabId)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        childFragmentManager.removeOnBackStackChangedListener(profileBackStackListener)
        _binding = null
        super.onDestroyView()
    }

    override fun openReportsScreen() {
        clearProfileBackStack()

        activeTabId = R.id.nav_reports
        binding.bottomNavigation.menu.findItem(R.id.nav_reports)?.isChecked = true

        showHomeFragment(TAG_REPORTS) {
            ReportsFragment()
        }

        updateBottomBarVisibility()
    }

    companion object {
        private const val KEY_ACTIVE_TAB_ID = "active_tab_id"
        private const val TAG = "HomeFragment"

        private const val TAG_SOCIAL = "home_tab_social"
        private const val TAG_DAILY = "home_tab_daily"
        private const val TAG_RECIPES = "home_tab_recipes"
        private const val TAG_REPORTS = "home_tab_reports"
        private const val TAG_PROFILE = "home_tab_profile"
        private const val TAG_PROFILE_WEIGHT = "home_tab_profile_weight"
        private const val TAG_PROFILE_BODY_MEASUREMENTS = "home_tab_profile_body_measurements"
        private const val TAG_PROFILE_DETAIL_PREFIX = "home_tab_profile_detail_"
        private const val TAG_PLACEHOLDER_VIEW = "home_tab_placeholder_view"

        private const val BACK_STACK_PROFILE_WEIGHT = "profile_weight"
        private const val BACK_STACK_PROFILE_BODY_MEASUREMENTS = "profile_body_measurements"
        private const val BACK_STACK_PROFILE_DETAIL = "profile_detail"

        private const val PROFILE_SCREEN_MAIN = "profile_main"
        private const val PROFILE_SCREEN_WEIGHT = "profile_weight"
        private const val PROFILE_SCREEN_BODY_MEASUREMENTS = "profile_body_measurements"
        private const val PROFILE_SCREEN_DETAIL_PREFIX = "profile_detail:"
    }

}
