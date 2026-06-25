package com.example.fitto.feature_dietplans

import com.example.fitto.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanDefaultResolverTest {

    @Test
    fun resolve_usesFallbacksWhenProfileIsMissing() {
        val defaults = CreateDietPlanDefaultResolver.resolve(profile = null)

        assertEquals(1800, defaults.dailyCalories)
        assertEquals(CreatePlanGoal.WEIGHT_LOSS, defaults.goal)
    }

    @Test
    fun resolve_usesPositiveDailyCalorieTarget() {
        val defaults = CreateDietPlanDefaultResolver.resolve(
            profile = profile(
                goalType = "GAIN_WEIGHT",
                dailyCalorieTarget = 2300
            )
        )

        assertEquals(2300, defaults.dailyCalories)
        assertEquals(CreatePlanGoal.WEIGHT_GAIN, defaults.goal)
    }

    @Test
    fun resolve_mapsProfileGoalsToCreatePlanGoals() {
        assertEquals(
            CreatePlanGoal.MUSCLE,
            CreateDietPlanDefaultResolver.resolve(
                profile(goalType = "MUSCLE_GAIN", dailyCalorieTarget = 1800)
            ).goal
        )
        assertEquals(
            CreatePlanGoal.MAINTAIN,
            CreateDietPlanDefaultResolver.resolve(
                profile(goalType = "MAINTAIN_WEIGHT", dailyCalorieTarget = 1800)
            ).goal
        )
        assertEquals(
            CreatePlanGoal.BALANCED,
            CreateDietPlanDefaultResolver.resolve(
                profile(goalType = "LEARN_NUTRITION", dailyCalorieTarget = 1800)
            ).goal
        )
    }

    @Test
    fun resolve_ignoresInvalidCalorieTargets() {
        val defaults = CreateDietPlanDefaultResolver.resolve(
            profile = profile(
                goalType = "EAT_HEALTHY",
                dailyCalorieTarget = 0
            )
        )

        assertEquals(1800, defaults.dailyCalories)
        assertEquals(CreatePlanGoal.BALANCED, defaults.goal)
    }

    private fun profile(
        goalType: String,
        dailyCalorieTarget: Int
    ): UserProfileEntity {
        return UserProfileEntity(
            userId = "user",
            email = "user@example.com",
            displayName = "User",
            userName = "user",
            heightCm = 170,
            weightKg = 70,
            targetWeightKg = null,
            age = 30,
            gender = "FEMALE",
            goalType = goalType,
            activityLevel = "MODERATE",
            bmr = 1400,
            maintenanceCalories = 2000,
            targetCalories = dailyCalorieTarget,
            recommendedCalories = dailyCalorieTarget,
            dailyCalorieTarget = dailyCalorieTarget,
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
