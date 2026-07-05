package com.example.fitto.feature_auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OnboardingViewModelFactory(
    private val aiRepository: OnboardingAiRepository,
    private val planEngineFactory: () -> OnboardingPlanEngine = ::OnboardingPlanEngine
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            "Unsupported ViewModel type: ${modelClass.name}"
        }
        return OnboardingViewModel(
            aiRepository = aiRepository,
            planEngine = planEngineFactory()
        ) as T
    }
}
