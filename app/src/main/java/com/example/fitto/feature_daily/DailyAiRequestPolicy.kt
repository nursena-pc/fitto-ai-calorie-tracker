package com.example.fitto.feature_daily

object DailyAiRequestPolicy {

    sealed interface Decision {
        data object Start : Decision
        data class BlockedByCooldown(val remainingSeconds: Long) : Decision
        data object AlreadyLoading : Decision
        data object SameRequestInFlight : Decision
    }

    fun resolve(
        cooldownUntilMillis: Long,
        nowMillis: Long,
        isLoading: Boolean,
        inFlightRequestKey: String?,
        requestKey: String
    ): Decision {
        if (cooldownUntilMillis > nowMillis) {
            return Decision.BlockedByCooldown(
                remainingSeconds = ((cooldownUntilMillis - nowMillis) / MILLIS_PER_SECOND)
                    .coerceAtLeast(MIN_REMAINING_SECONDS)
            )
        }

        if (isLoading) return Decision.AlreadyLoading
        if (inFlightRequestKey == requestKey) return Decision.SameRequestInFlight

        return Decision.Start
    }

    private const val MILLIS_PER_SECOND = 1000L
    private const val MIN_REMAINING_SECONDS = 1L
}
