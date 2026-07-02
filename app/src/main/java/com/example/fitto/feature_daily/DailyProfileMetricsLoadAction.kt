package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity

internal class DailyProfileMetricsLoadAction(
    private val loadProfile: suspend () -> UserProfileEntity?
) {

    suspend fun load(
        fallbackWeightKg: Double
    ): DailyProfileMetricsLoadResult {
        val profile = loadProfile()
        val profileWeightKg = DailyProfileMetricsResolver.resolveWeightKg(profile)

        return DailyProfileMetricsLoadResult(
            profileWeightKg = profileWeightKg,
            displayWeightKg = profileWeightKg ?: fallbackWeightKg,
            waterGoalMl = DailyProfileMetricsResolver.resolveWaterGoalMl(profile)
        )
    }
}

internal data class DailyProfileMetricsLoadResult(
    val profileWeightKg: Double?,
    val displayWeightKg: Double,
    val waterGoalMl: Int
)
