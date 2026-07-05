package com.example.fitto.feature_dietplans

import com.example.fitto.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DietPlanProfileLoaderTest {

    @Test
    fun `cached profile is returned without remote sync`() = runBlocking {
        val cached = profile("cached")
        val source = FakeProfileSource(cached = cached, synced = profile("synced"))
        val loader = DietPlanProfileLoader(source, Dispatchers.Unconfined)

        val result = loader.loadCurrentProfile()

        assertSame(cached, result)
        assertEquals(1, source.cachedCalls)
        assertEquals(0, source.syncCalls)
    }

    @Test
    fun `missing cache falls back to remote sync`() = runBlocking {
        val synced = profile("synced")
        val source = FakeProfileSource(cached = null, synced = synced)
        val loader = DietPlanProfileLoader(source, Dispatchers.Unconfined)

        val result = loader.loadCurrentProfile()

        assertSame(synced, result)
        assertEquals(1, source.cachedCalls)
        assertEquals(1, source.syncCalls)
    }

    private fun profile(id: String): UserProfileEntity {
        return UserProfileEntity(
            userId = id,
            email = "$id@example.com",
            displayName = id,
            userName = id,
            heightCm = 170,
            weightKg = 70,
            targetWeightKg = null,
            age = 30,
            gender = "FEMALE",
            goalType = "MAINTAIN_WEIGHT",
            activityLevel = "MODERATE",
            bmr = 1400,
            maintenanceCalories = 2000,
            targetCalories = 1800,
            recommendedCalories = 1800,
            dailyCalorieTarget = 1800,
            proteinTargetGrams = 120,
            carbTargetGrams = 200,
            fatTargetGrams = 60,
            waterTargetMl = 2000,
            weeklyWeightChangeKg = 0.0,
            estimatedWeeks = null,
            planStatus = "",
            profileCompleted = true,
            updatedAt = 0L,
            syncedAt = 0L
        )
    }

    private class FakeProfileSource(
        private val cached: UserProfileEntity?,
        private val synced: UserProfileEntity?
    ) : DietPlanProfileLoader.ProfileSource {
        var cachedCalls = 0
        var syncCalls = 0

        override suspend fun getCachedProfile(): UserProfileEntity? {
            cachedCalls += 1
            return cached
        }

        override suspend fun syncProfile(): UserProfileEntity? {
            syncCalls += 1
            return synced
        }
    }
}
