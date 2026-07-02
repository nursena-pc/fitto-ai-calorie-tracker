package com.example.fitto.feature_home.presentation.profile

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDataExportFileWriterTest {

    private val writer = ProfileDataExportFileWriter()

    @Test
    fun `writer creates timestamped json file with existing export shape`() {
        val directory = Files.createTempDirectory("fitto-export").toFile()
        try {
            val file = writer.write(directory, snapshot())
            val json = file.readText()

            assertEquals("fitto_profile_export_1234.json", file.name)
            assertTrue(json.contains("\"exportedAt\": 1234"))
            assertTrue(json.contains("\"id\": \"user-1\""))
            assertTrue(json.contains("\"currentWeightKg\": 72.5"))
            assertTrue(json.contains("\"waistCm\": 81"))
            assertTrue(json.contains("\"id\": \"history-1\""))
            assertTrue(json.contains("\"id\": \"notification-1\""))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `writer creates missing export directory`() {
        val parent = Files.createTempDirectory("fitto-export-parent").toFile()
        val directory = parent.resolve("nested/exports")
        try {
            val file = writer.write(directory, snapshot())

            assertTrue(directory.isDirectory)
            assertTrue(file.isFile)
        } finally {
            parent.deleteRecursively()
        }
    }

    private fun snapshot() = ProfileDataExportSnapshot(
        exportedAt = 1234L,
        user = ProfileDataExportUser(
            id = "user-1",
            email = "fitto@example.com",
            visibleName = "visible",
            userPersonalName = "personal",
            photoUrl = "photo.jpg"
        ),
        health = ProfileDataExportHealth(
            currentWeightKg = 72.5,
            initialWeightKg = 80.0,
            targetWeightKg = 68.0,
            heightCm = 175,
            age = 29,
            dailyTargetCalories = 2100,
            waterTargetLiter = 2.7,
            proteinGoalGrams = 140,
            carbGoalGrams = 220,
            fatGoalGrams = 65
        ),
        bodyMeasurements = mapOf(
            "waistCm" to 81f,
            "updatedAt" to 100L
        ),
        bodyMeasurementHistory = listOf(
            ProfileDataExportBodyMeasurementHistory(
                id = "history-1",
                createdAt = 100L,
                measurements = mapOf("waistCm" to 81f, "updatedAt" to 100L)
            )
        ),
        notifications = listOf(
            ProfileDataExportNotification(
                id = "notification-1",
                title = "Title",
                description = "Description",
                type = "general",
                createdAt = 100L,
                isRead = false
            )
        )
    )
}
