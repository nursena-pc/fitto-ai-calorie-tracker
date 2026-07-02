package com.example.fitto.feature_home.presentation.profile

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProfileDataExportFileWriter {

    fun write(
        exportDirectory: File,
        snapshot: ProfileDataExportSnapshot
    ): File {
        val exportJson = JSONObject()
            .put("exportedAt", snapshot.exportedAt)
            .put("user", JSONObject()
                .put("id", snapshot.user.id)
                .put("email", snapshot.user.email)
                .put("visibleName", snapshot.user.visibleName)
                .put("userPersonalName", snapshot.user.userPersonalName)
                .put("photoUrl", snapshot.user.photoUrl)
            )
            .put("health", JSONObject()
                .put("currentWeightKg", snapshot.health.currentWeightKg)
                .put("initialWeightKg", snapshot.health.initialWeightKg)
                .put("targetWeightKg", snapshot.health.targetWeightKg)
                .put("heightCm", snapshot.health.heightCm)
                .put("age", snapshot.health.age)
                .put("dailyTargetCalories", snapshot.health.dailyTargetCalories)
                .put("waterTargetLiter", snapshot.health.waterTargetLiter)
                .put("proteinGoalGrams", snapshot.health.proteinGoalGrams)
                .put("carbGoalGrams", snapshot.health.carbGoalGrams)
                .put("fatGoalGrams", snapshot.health.fatGoalGrams)
            )
            .put("bodyMeasurements", JSONObject(snapshot.bodyMeasurements))
            .put("bodyMeasurementHistory", JSONArray().apply {
                snapshot.bodyMeasurementHistory.forEach { item ->
                    put(JSONObject()
                        .put("id", item.id)
                        .put("createdAt", item.createdAt)
                        .put("measurements", JSONObject(item.measurements))
                    )
                }
            })
            .put("notifications", JSONArray().apply {
                snapshot.notifications.forEach { notification ->
                    put(JSONObject()
                        .put("id", notification.id)
                        .put("title", notification.title)
                        .put("description", notification.description)
                        .put("type", notification.type)
                        .put("createdAt", notification.createdAt)
                        .put("isRead", notification.isRead)
                    )
                }
            })

        exportDirectory.mkdirs()
        val exportFile = File(
            exportDirectory,
            "fitto_profile_export_${snapshot.exportedAt}.json"
        )
        exportFile.writeText(exportJson.toString(2))
        return exportFile
    }
}
