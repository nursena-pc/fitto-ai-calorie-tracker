package com.example.fitto.feature_auth.register

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitto.fittoAppContainer

internal fun Fragment.obtainOnboardingViewModel(): OnboardingViewModel {
    val factory = requireContext()
        .fittoAppContainer
        .authFeatureDependencies
        .onboardingViewModelFactory
    return ViewModelProvider(requireActivity(), factory)[OnboardingViewModel::class.java]
}
