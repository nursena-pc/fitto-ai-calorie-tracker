package com.example.fitto.feature_daily

import java.time.LocalDate

object DailyStepCounterStartPolicy {

    sealed interface Decision {
        data object StopForPastDate : Decision
        data object SensorUnavailable : Decision
        data object RequestActivityRecognitionPermission : Decision
        data object Start : Decision
    }

    fun resolve(
        selectedDate: LocalDate,
        today: LocalDate,
        isStepCounterAvailable: Boolean,
        requiresActivityRecognitionPermission: Boolean,
        hasActivityRecognitionPermission: Boolean
    ): Decision {
        if (selectedDate != today) {
            return Decision.StopForPastDate
        }

        if (!isStepCounterAvailable) {
            return Decision.SensorUnavailable
        }

        if (requiresActivityRecognitionPermission && !hasActivityRecognitionPermission) {
            return Decision.RequestActivityRecognitionPermission
        }

        return Decision.Start
    }
}
