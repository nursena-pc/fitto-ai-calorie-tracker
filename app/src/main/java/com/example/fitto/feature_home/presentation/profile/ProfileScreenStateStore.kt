package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.SocialUser

internal data class ProfileScreenState(
    val user: SocialUser? = null,
    val postCount: Int = 0,
    val healthData: ProfileHealthData = ProfileHealthData.Empty,
    val weeklySummary: ProfileWeeklySummary = ProfileWeeklySummary.Empty,
    val scrollY: Int = 0
)

internal class ProfileScreenStateStore(
    initialState: ProfileScreenState = ProfileScreenState()
) {
    var state: ProfileScreenState = initialState
        private set

    fun applyProfile(uiState: ProfileUiState): ProfileScreenState {
        state = state.copy(
            user = uiState.user ?: state.user,
            postCount = uiState.postCount,
            healthData = uiState.healthData
        )
        return state
    }

    fun updateWeeklySummary(summary: ProfileWeeklySummary): ProfileScreenState {
        state = state.copy(weeklySummary = summary)
        return state
    }

    fun updateScrollY(scrollY: Int): ProfileScreenState {
        state = state.copy(scrollY = scrollY.coerceAtLeast(0))
        return state
    }
}
