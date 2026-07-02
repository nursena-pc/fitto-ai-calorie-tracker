package com.example.fitto.feature_daily

internal object DailyWeightCardStateResolver {

    sealed interface State {
        data object Unknown : State

        data class Available(
            val weightKg: Double
        ) : State
    }

    fun resolve(weightKg: Double?): State {
        return weightKg
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?.let { State.Available(it) }
            ?: State.Unknown
    }
}
