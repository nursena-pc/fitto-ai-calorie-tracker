package com.example.fitto.feature_daily

class DailyFastingCountdownCoordinator(
    private val timerFactory: DailyCountdownTimerFactory,
    private val onTick: (planId: String, remainingMillis: Long) -> Unit,
    private val onFinish: (planId: String) -> Unit
) {

    private var timer: DailyCountdownTimer? = null
    private var generation: Long = 0L

    fun start(
        planId: String,
        remainingMillis: Long
    ) {
        stop()

        if (remainingMillis <= 0L) {
            return
        }

        val timerGeneration = generation
        val createdTimer = timerFactory.create(
            DailyCountdownTimerRequest(
                durationMillis = remainingMillis,
                intervalMillis = TICK_INTERVAL_MILLIS,
                onTick = { millisUntilFinished ->
                    if (timerGeneration == generation) {
                        onTick(planId, millisUntilFinished.coerceAtLeast(0L))
                    }
                },
                onFinish = {
                    if (timerGeneration == generation) {
                        timer = null
                        generation += 1L
                        onFinish(planId)
                    }
                }
            )
        )
        timer = createdTimer
        createdTimer.start()
    }

    fun stop() {
        generation += 1L
        timer?.cancel()
        timer = null
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 1_000L
    }
}
