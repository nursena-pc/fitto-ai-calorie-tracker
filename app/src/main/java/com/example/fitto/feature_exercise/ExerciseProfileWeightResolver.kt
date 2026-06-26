package com.example.fitto.feature_exercise

import com.example.fitto.data.local.entity.UserProfileEntity
import java.util.Locale
import kotlin.math.roundToInt

object ExerciseProfileWeightResolver {

    fun resolveWeightKg(profile: UserProfileEntity?): Double? {
        return profile
            ?.weightKg
            ?.toDouble()
            ?.takeIf { weightKg -> weightKg in MIN_USER_WEIGHT_KG..MAX_USER_WEIGHT_KG }
    }

    fun formatWeight(
        value: Double,
        locale: Locale = Locale.getDefault()
    ): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format(locale, "%.1f", value)
        }
    }

    private const val MIN_USER_WEIGHT_KG = 25.0
    private const val MAX_USER_WEIGHT_KG = 300.0
}
