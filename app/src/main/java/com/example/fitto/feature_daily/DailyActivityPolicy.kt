package com.example.fitto.feature_daily

object DailyActivityPolicy {

    enum class Source {
        HEALTH_CONNECT,
        FITTO_COUNTER,
        MANUAL_ONLY,
        CONNECT
    }

    fun resolveDisplaySteps(
        isHealthConnectAvailable: Boolean,
        hasHealthConnectPermission: Boolean,
        healthSteps: Long,
        localSteps: Long
    ): Long {
        val safeHealthSteps = if (isHealthConnectAvailable && hasHealthConnectPermission) {
            healthSteps.coerceAtLeast(0L)
        } else {
            0L
        }

        return if (safeHealthSteps > 0L) {
            safeHealthSteps
        } else {
            localSteps.coerceAtLeast(0L)
        }
    }

    fun resolveBurnedCalories(
        healthActiveCalories: Int,
        manualExerciseCalories: Int
    ): Int {
        return maxOf(
            healthActiveCalories.coerceAtLeast(0),
            manualExerciseCalories.coerceAtLeast(0)
        )
    }

    fun resolveActivitySource(
        isHealthConnectAvailable: Boolean,
        hasHealthConnectPermission: Boolean,
        healthSteps: Long,
        healthActiveCalories: Int,
        localSteps: Long,
        manualExerciseCalories: Int
    ): Source {
        val hasHealthPermission = isHealthConnectAvailable && hasHealthConnectPermission
        val hasHealthData = hasHealthPermission &&
            (healthSteps > 0L || healthActiveCalories > 0)
        val hasLocalSteps = localSteps > 0L
        val hasManualExercise = manualExerciseCalories > 0

        return when {
            hasHealthData -> Source.HEALTH_CONNECT
            hasLocalSteps -> Source.FITTO_COUNTER
            hasManualExercise -> Source.MANUAL_ONLY
            else -> Source.CONNECT
        }
    }
}
