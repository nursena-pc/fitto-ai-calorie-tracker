package com.example.fitto.feature_daily

import com.example.fitto.feature_health.HealthConnectDailySummary

object DailyActivityPresentationResolver {

    fun resolveDisplaySteps(
        healthConnectSummary: HealthConnectDailySummary,
        localSteps: Long
    ): Long {
        return DailyActivityPolicy.resolveDisplaySteps(
            isHealthConnectAvailable = healthConnectSummary.isAvailable,
            hasHealthConnectPermission = healthConnectSummary.hasPermission,
            healthSteps = healthConnectSummary.steps,
            localSteps = localSteps
        )
    }

    fun resolveSource(
        healthConnectSummary: HealthConnectDailySummary,
        healthActiveCalories: Int,
        localSteps: Long,
        manualExerciseCalories: Int
    ): DailyActivityPolicy.Source {
        return DailyActivityPolicy.resolveActivitySource(
            isHealthConnectAvailable = healthConnectSummary.isAvailable,
            hasHealthConnectPermission = healthConnectSummary.hasPermission,
            healthSteps = healthConnectSummary.steps,
            healthActiveCalories = healthActiveCalories,
            localSteps = localSteps,
            manualExerciseCalories = manualExerciseCalories
        )
    }
}
