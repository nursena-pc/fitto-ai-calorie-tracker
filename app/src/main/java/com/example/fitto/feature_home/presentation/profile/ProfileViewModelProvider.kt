package com.example.fitto.feature_home.presentation.profile

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitto.fittoAppContainer

internal fun Fragment.obtainProfileViewModel(): ProfileViewModel {
    val factory = requireContext()
        .fittoAppContainer
        .profileFeatureDependencies
        .viewModelFactory
    return ViewModelProvider(requireActivity(), factory)[ProfileViewModel::class.java]
}
