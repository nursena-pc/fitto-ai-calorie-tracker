package com.example.fitto.data.local.profile

import com.example.fitto.data.local.entity.UserProfileEntity

object FirebaseUserProfileMapper {

    fun fromFirebaseMap(
        userId: String,
        emailFallback: String,
        data: Map<String, Any?>
    ): UserProfileEntity {
        val targets = data["targets"] as? Map<*, *>
        val currentTarget = targets?.get("current") as? Map<*, *>
        val macroTargets = currentTarget?.get("macroTargets") as? Map<*, *>

        val dailyCalorieTarget = currentTarget.intValue("appliedCalories")
            .takeIf { it > 0 }
            ?: currentTarget.intValue("targetCalories")

        val weightKg = data.intValue("weightKg")
        val waterTargetMl = calculateWaterTargetMl(weightKg)

        return UserProfileEntity(
            userId = userId,
            email = data.stringValue("email").ifBlank { emailFallback },
            displayName = data.stringValue("displayName")
                .ifBlank { data.stringValue("userName") }
                .ifBlank { data.stringValue("username") },
            userName = data.stringValue("userName")
                .ifBlank { data.stringValue("username") }
                .ifBlank { data.stringValue("displayName") },

            heightCm = data.intValue("heightCm"),
            weightKg = weightKg,
            targetWeightKg = data.intValueOrNull("targetWeightKg"),
            age = data.intValue("age"),
            gender = data.stringValue("gender"),
            goalType = data.stringValue("goalType"),
            activityLevel = data.stringValue("activityLevel"),

            bmr = currentTarget.intValue("bmr"),
            maintenanceCalories = currentTarget.intValue("maintenanceCalories"),
            targetCalories = currentTarget.intValue("targetCalories"),
            recommendedCalories = currentTarget.intValue("recommendedCalories"),
            dailyCalorieTarget = dailyCalorieTarget,

            proteinTargetGrams = macroTargets.intValue("proteinGrams"),
            carbTargetGrams = macroTargets.intValue("carbGrams"),
            fatTargetGrams = macroTargets.intValue("fatGrams"),

            waterTargetMl = waterTargetMl,
            weeklyWeightChangeKg = currentTarget.doubleValue("weeklyWeightChangeKg"),
            estimatedWeeks = currentTarget.intValueOrNull("estimatedWeeks"),
            planStatus = currentTarget.stringValue("planStatus"),

            profileCompleted = data.booleanValue("profileCompleted"),
            updatedAt = data.longValue("updatedAt"),
            syncedAt = System.currentTimeMillis()
        )
    }

    private fun calculateWaterTargetMl(weightKg: Int): Int {
        if (weightKg <= 0) return 2000
        return (weightKg * 35).coerceIn(1500, 4500)
    }

    private fun Map<*, *>?.stringValue(key: String): String {
        return this?.get(key)?.toString().orEmpty()
    }

    private fun Map<*, *>?.booleanValue(key: String): Boolean {
        return this?.get(key) as? Boolean ?: false
    }

    private fun Map<*, *>?.intValue(key: String): Int {
        return (this?.get(key) as? Number)?.toInt() ?: 0
    }

    private fun Map<*, *>?.intValueOrNull(key: String): Int? {
        return (this?.get(key) as? Number)?.toInt()
    }

    private fun Map<*, *>?.longValue(key: String): Long {
        return (this?.get(key) as? Number)?.toLong() ?: 0L
    }

    private fun Map<*, *>?.doubleValue(key: String): Double {
        return (this?.get(key) as? Number)?.toDouble() ?: 0.0
    }
}