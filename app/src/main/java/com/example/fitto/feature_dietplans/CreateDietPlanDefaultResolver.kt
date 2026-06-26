package com.example.fitto.feature_dietplans

import com.example.fitto.data.local.entity.UserProfileEntity

object CreateDietPlanDefaultResolver {

    fun resolve(profile: UserProfileEntity?): CreateDietPlanDefaults {
        return CreateDietPlanDefaults(
            dailyCalories = profile?.dailyCalorieTarget
                ?.takeIf { calories -> calories > 0 }
                ?: DEFAULT_DAILY_CALORIES,
            goal = resolveGoal(profile?.goalType.orEmpty())
        )
    }

    private fun resolveGoal(goalType: String): CreatePlanGoal {
        return when (goalType.uppercase()) {
            "GAIN_WEIGHT" -> CreatePlanGoal.WEIGHT_GAIN
            "MUSCLE", "MUSCLE_GAIN" -> CreatePlanGoal.MUSCLE
            "MAINTAIN_WEIGHT" -> CreatePlanGoal.MAINTAIN
            "EAT_HEALTHY", "LEARN_NUTRITION" -> CreatePlanGoal.BALANCED
            else -> CreatePlanGoal.WEIGHT_LOSS
        }
    }

    private const val DEFAULT_DAILY_CALORIES = 1800
}
