package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.FittoNotification
import com.example.fitto.feature_home.presentation.social.SocialUser

data class ProfileDataExportRequest(
    val user: SocialUser?,
    val healthData: ProfileHealthData,
    val bodyMeasurements: BodyMeasurementsData,
    val bodyMeasurementHistory: List<BodyMeasurementsHistoryItem>
)

data class ProfileDataExportSnapshot(
    val exportedAt: Long,
    val user: ProfileDataExportUser,
    val health: ProfileDataExportHealth,
    val bodyMeasurements: Map<String, Any?>,
    val bodyMeasurementHistory: List<ProfileDataExportBodyMeasurementHistory>,
    val notifications: List<ProfileDataExportNotification>
)

data class ProfileDataExportUser(
    val id: String,
    val email: String,
    val visibleName: String,
    val userPersonalName: String,
    val photoUrl: String
)

data class ProfileDataExportHealth(
    val currentWeightKg: Double,
    val initialWeightKg: Double,
    val targetWeightKg: Double,
    val heightCm: Int,
    val age: Int,
    val dailyTargetCalories: Int,
    val waterTargetLiter: Double,
    val proteinGoalGrams: Int,
    val carbGoalGrams: Int,
    val fatGoalGrams: Int
)

data class ProfileDataExportBodyMeasurementHistory(
    val id: String,
    val createdAt: Long,
    val measurements: Map<String, Any?>
)

data class ProfileDataExportNotification(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val createdAt: Long,
    val isRead: Boolean
)

internal fun FittoNotification.toProfileDataExportNotification() = ProfileDataExportNotification(
    id = id,
    title = title,
    description = description,
    type = type,
    createdAt = createdAt,
    isRead = isRead
)
