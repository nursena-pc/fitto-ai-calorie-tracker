package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.SocialUser

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: SocialUser? = null,
    val postCount: Int = 0,
    val healthData: ProfileHealthData = ProfileHealthData.Empty,
    val bodyMeasurements: BodyMeasurementsData = BodyMeasurementsData(),
    val bodyMeasurementsHistory: List<BodyMeasurementsHistoryItem> = emptyList(),
    val errorMessage: String? = null
)