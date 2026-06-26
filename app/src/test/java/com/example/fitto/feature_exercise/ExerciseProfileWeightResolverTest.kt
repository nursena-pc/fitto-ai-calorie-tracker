package com.example.fitto.feature_exercise

import com.example.fitto.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ExerciseProfileWeightResolverTest {

    @Test
    fun resolveWeightKg_returnsProfileWeightInsideSupportedRange() {
        assertEquals(
            72.0,
            ExerciseProfileWeightResolver.resolveWeightKg(profile(weightKg = 72)) ?: 0.0,
            0.0
        )
    }

    @Test
    fun resolveWeightKg_rejectsOutOfRangeProfileWeight() {
        assertNull(ExerciseProfileWeightResolver.resolveWeightKg(profile(weightKg = 20)))
        assertNull(ExerciseProfileWeightResolver.resolveWeightKg(profile(weightKg = 350)))
    }

    @Test
    fun formatWeight_removesTrailingDecimalForWholeNumbers() {
        assertEquals(
            "70",
            ExerciseProfileWeightResolver.formatWeight(70.0, Locale.US)
        )
        assertEquals(
            "70.5",
            ExerciseProfileWeightResolver.formatWeight(70.5, Locale.US)
        )
    }

    private fun profile(weightKg: Int): UserProfileEntity {
        return UserProfileEntity(
            userId = "user",
            email = "user@example.com",
            displayName = "User",
            userName = "user",
            heightCm = 170,
            weightKg = weightKg,
            targetWeightKg = null,
            age = 30,
            gender = "FEMALE",
            goalType = "MAINTAIN",
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
}
