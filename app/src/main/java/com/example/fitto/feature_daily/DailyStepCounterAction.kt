package com.example.fitto.feature_daily

import java.time.LocalDate

class DailyStepCounterAction(
    private val getCachedSteps: (LocalDate) -> Long,
    private val isStepCounterAvailable: () -> Boolean,
    private val startStepCounter: (LocalDate, (Long) -> Unit) -> Unit,
    private val stopStepCounter: () -> Unit
) {

    fun start(
        request: DailyStepCounterRequest,
        onStepsChanged: (DailyStepCounterUpdate) -> Unit
    ): DailyStepCounterEffect {
        onStepsChanged(
            DailyStepCounterUpdate(
                steps = getCachedSteps(request.selectedDate).coerceAtLeast(0L),
                shouldRefreshSummary = false
            )
        )

        return when (
            DailyStepCounterStartPolicy.resolve(
                selectedDate = request.selectedDate,
                today = request.today,
                isStepCounterAvailable = isStepCounterAvailable(),
                requiresActivityRecognitionPermission = request.requiresActivityRecognitionPermission,
                hasActivityRecognitionPermission = request.hasActivityRecognitionPermission
            )
        ) {
            DailyStepCounterStartPolicy.Decision.StopForPastDate -> {
                stopStepCounter()
                DailyStepCounterEffect.None
            }

            DailyStepCounterStartPolicy.Decision.SensorUnavailable -> {
                DailyStepCounterEffect.SensorUnavailable
            }

            DailyStepCounterStartPolicy.Decision.RequestActivityRecognitionPermission -> {
                DailyStepCounterEffect.RequestActivityRecognitionPermission
            }

            DailyStepCounterStartPolicy.Decision.Start -> {
                startStepCounter(request.selectedDate) { steps ->
                    onStepsChanged(
                        DailyStepCounterUpdate(
                            steps = steps.coerceAtLeast(0L),
                            shouldRefreshSummary = true
                        )
                    )
                }
                DailyStepCounterEffect.None
            }
        }
    }

    fun stop() {
        stopStepCounter()
    }
}

data class DailyStepCounterRequest(
    val selectedDate: LocalDate,
    val today: LocalDate,
    val requiresActivityRecognitionPermission: Boolean,
    val hasActivityRecognitionPermission: Boolean
)

data class DailyStepCounterUpdate(
    val steps: Long,
    val shouldRefreshSummary: Boolean
)

sealed interface DailyStepCounterEffect {
    data object None : DailyStepCounterEffect
    data object SensorUnavailable : DailyStepCounterEffect
    data object RequestActivityRecognitionPermission : DailyStepCounterEffect
}
