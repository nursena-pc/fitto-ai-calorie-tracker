package com.example.fitto

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.fitto.feature_home.presentation.HomeFragment
import com.example.fitto.feature_home.presentation.navigation.HomeNavigationRequest

internal class HomeFragmentDispatcher(
    private val activity: AppCompatActivity
) {

    fun dispatch(request: HomeNavigationRequest) {
        dispatchWhenReady(attempt = 0) { homeFragment ->
            homeFragment.navigate(request)
        }
    }

    fun notifyAccountScopeChanged() {
        findAnyHomeFragment()?.onAccountScopeChanged()
    }

    fun rememberCurrentScreenForSettingsRecreate() {
        findPrimaryHomeFragment()?.rememberCurrentScreenForSettingsRecreate()
    }

    private fun dispatchWhenReady(
        attempt: Int,
        action: (HomeFragment) -> Unit
    ) {
        activity.window.decorView.postDelayed(
            {
                val homeFragment = findPrimaryHomeFragment()

                if (homeFragment != null) {
                    action(homeFragment)
                } else if (HomeFragmentDispatchPolicy.shouldRetry(attempt)) {
                    dispatchWhenReady(attempt + 1, action)
                }
            },
            HomeFragmentDispatchPolicy.delayMillis(attempt)
        )
    }

    private fun findPrimaryHomeFragment(): HomeFragment? {
        return findNavHostFragment()
            ?.childFragmentManager
            ?.primaryNavigationFragment as? HomeFragment
    }

    private fun findAnyHomeFragment(): HomeFragment? {
        return findNavHostFragment()
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { it is HomeFragment } as? HomeFragment
    }

    private fun findNavHostFragment(): NavHostFragment? {
        return activity.supportFragmentManager
            .findFragmentById(R.id.mainNavHost) as? NavHostFragment
    }
}
