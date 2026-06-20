package com.example.fitto.feature_daily

import com.example.fitto.feature_sleep.data.SleepLogLocalModel

object DailySleepCardStateResolver {

    sealed interface State {
        data object Empty : State

        data class Logged(
            val durationMinutes: Int,
            val selection: DailySleepTimeSelection
        ) : State
    }

    fun resolve(log: SleepLogLocalModel?): State {
        return log?.let {
            State.Logged(
                durationMinutes = it.durationMinutes,
                selection = DailySleepTimeSelection.from(it)
            )
        } ?: State.Empty
    }
}
