package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity

internal fun testProfile(
    userId: String,
    weightKg: Int = 70,
    dailyCalorieTarget: Int = 1800,
    proteinTargetGrams: Int = 120,
    carbTargetGrams: Int = 200,
    fatTargetGrams: Int = 60,
    waterTargetMl: Int = 2500
): UserProfileEntity {
    return UserProfileEntity(
        userId = userId,
        email = "$userId@example.com",
        displayName = userId,
        userName = userId,
        heightCm = 170,
        weightKg = weightKg,
        targetWeightKg = null,
        age = 30,
        gender = "unspecified",
        goalType = "maintain",
        activityLevel = "moderate",
        bmr = 1500,
        maintenanceCalories = 2000,
        targetCalories = dailyCalorieTarget,
        recommendedCalories = dailyCalorieTarget,
        dailyCalorieTarget = dailyCalorieTarget,
        proteinTargetGrams = proteinTargetGrams,
        carbTargetGrams = carbTargetGrams,
        fatTargetGrams = fatTargetGrams,
        waterTargetMl = waterTargetMl,
        weeklyWeightChangeKg = 0.0,
        estimatedWeeks = null,
        planStatus = "HEALTHY",
        profileCompleted = true,
        updatedAt = 1L,
        syncedAt = 1L
    )
}
