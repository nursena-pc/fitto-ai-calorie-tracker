package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.FittoNotification

class ProfileDataExportSnapshotFactory {

    fun create(
        request: ProfileDataExportRequest,
        notifications: List<FittoNotification>,
        exportedAt: Long
    ): ProfileDataExportSnapshot {
        val user = request.user
        val health = request.healthData

        return ProfileDataExportSnapshot(
            exportedAt = exportedAt,
            user = ProfileDataExportUser(
                id = user?.id.orEmpty(),
                email = user?.email.orEmpty(),
                visibleName = user?.userName.orEmpty(),
                userPersonalName = user?.personalName.orEmpty(),
                photoUrl = user?.photoUrl.orEmpty()
            ),
            health = ProfileDataExportHealth(
                currentWeightKg = health.currentWeightKg,
                initialWeightKg = health.initialWeightKg,
                targetWeightKg = health.targetWeightKg,
                heightCm = health.heightCm,
                age = health.age,
                dailyTargetCalories = health.dailyTargetCalories,
                waterTargetLiter = health.waterTargetLiter,
                proteinGoalGrams = health.proteinGoalGrams,
                carbGoalGrams = health.carbGoalGrams,
                fatGoalGrams = health.fatGoalGrams
            ),
            bodyMeasurements = request.bodyMeasurements.toMap(),
            bodyMeasurementHistory = request.bodyMeasurementHistory.map { item ->
                ProfileDataExportBodyMeasurementHistory(
                    id = item.id,
                    createdAt = item.createdAt,
                    measurements = item.data.toMap()
                )
            },
            notifications = notifications.map { notification ->
                notification.toProfileDataExportNotification()
            }
        )
    }
}
