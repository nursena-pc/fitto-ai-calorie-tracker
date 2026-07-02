package com.example.fitto.feature_daily

fun interface DailyCountdownTimerFactory {
    fun create(request: DailyCountdownTimerRequest): DailyCountdownTimer
}

data class DailyCountdownTimerRequest(
    val durationMillis: Long,
    val intervalMillis: Long,
    val onTick: (Long) -> Unit,
    val onFinish: () -> Unit
)

interface DailyCountdownTimer {
    fun start()
    fun cancel()
}
