package com.example.fitto.feature_home.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class SocialViewModelFactory(
    private val repository: SocialRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SocialViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return SocialViewModel(repository) as T
    }
}
