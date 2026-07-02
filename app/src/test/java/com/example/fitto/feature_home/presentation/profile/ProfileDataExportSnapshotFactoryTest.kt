package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.FittoNotification
import com.example.fitto.feature_home.presentation.social.SocialUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDataExportSnapshotFactoryTest {

    private val factory = ProfileDataExportSnapshotFactory()

    @Test
    fun `snapshot maps user health and body measurement fields`() {
        val snapshot = factory.create(
            request = ProfileDataExportRequest(
                user = SocialUser(
                    id = "user-1",
                    userName = "visible",
                    personalName = "personal",
                    email = "fitto@example.com",
                    photoUrl = "photo.jpg"
                ),
                healthData = ProfileHealthData(
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
                bodyMeasurements = BodyMeasurementsData(
                    waistCm = 81f,
                    updatedAt = 100L
                ),
                bodyMeasurementHistory = emptyList()
            ),
            notifications = emptyList(),
            exportedAt = 500L
        )

        assertEquals(500L, snapshot.exportedAt)
        assertEquals("user-1", snapshot.user.id)
        assertEquals("visible", snapshot.user.visibleName)
        assertEquals("personal", snapshot.user.userPersonalName)
        assertEquals(72.5, snapshot.health.currentWeightKg, 0.0)
        assertEquals(2100, snapshot.health.dailyTargetCalories)
        assertEquals(81f, snapshot.bodyMeasurements["waistCm"])
        assertEquals(100L, snapshot.bodyMeasurements["updatedAt"])
    }

    @Test
    fun `missing user is exported with empty strings`() {
        val snapshot = factory.create(
            request = ProfileDataExportRequest(
                user = null,
                healthData = ProfileHealthData.Empty,
                bodyMeasurements = BodyMeasurementsData(updatedAt = 1L),
                bodyMeasurementHistory = emptyList()
            ),
            notifications = emptyList(),
            exportedAt = 1L
        )

        assertEquals("", snapshot.user.id)
        assertEquals("", snapshot.user.email)
        assertEquals("", snapshot.user.visibleName)
        assertEquals("", snapshot.user.userPersonalName)
        assertEquals("", snapshot.user.photoUrl)
    }

    @Test
    fun `history and notifications preserve source order and values`() {
        val snapshot = factory.create(
            request = ProfileDataExportRequest(
                user = null,
                healthData = ProfileHealthData.Empty,
                bodyMeasurements = BodyMeasurementsData(updatedAt = 1L),
                bodyMeasurementHistory = listOf(
                    BodyMeasurementsHistoryItem(
                        id = "second",
                        createdAt = 20L,
                        data = BodyMeasurementsData(chestCm = 90f, updatedAt = 20L)
                    ),
                    BodyMeasurementsHistoryItem(
                        id = "first",
                        createdAt = 10L,
                        data = BodyMeasurementsData(waistCm = 80f, updatedAt = 10L)
                    )
                )
            ),
            notifications = listOf(
                FittoNotification(
                    id = "n2",
                    title = "Second",
                    description = "Description 2",
                    iconRes = R.drawable.ic_notifications,
                    type = "social",
                    isRead = false,
                    createdAt = 20L
                ),
                FittoNotification(
                    id = "n1",
                    title = "First",
                    description = "Description 1",
                    iconRes = R.drawable.ic_notifications,
                    createdAt = 10L
                )
            ),
            exportedAt = 30L
        )

        assertEquals(listOf("second", "first"), snapshot.bodyMeasurementHistory.map { it.id })
        assertEquals(90f, snapshot.bodyMeasurementHistory.first().measurements["chestCm"])
        assertEquals(listOf("n2", "n1"), snapshot.notifications.map { it.id })
        assertEquals("Description 2", snapshot.notifications.first().description)
        assertTrue(!snapshot.notifications.first().isRead)
    }
}
