package com.example.fitto.feature_daily

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyStepCounterStartPolicyTest {

    private val today = LocalDate.of(2026, 6, 27)

    @Test
    fun resolve_stopsCounterForPastDates() {
        val decision = DailyStepCounterStartPolicy.resolve(
            selectedDate = today.minusDays(1),
            today = today,
            isStepCounterAvailable = true,
            requiresActivityRecognitionPermission = true,
            hasActivityRecognitionPermission = true
        )

        assertTrue(decision is DailyStepCounterStartPolicy.Decision.StopForPastDate)
    }

    @Test
    fun resolve_reportsUnavailableSensorForTodayWhenSensorIsMissing() {
        val decision = DailyStepCounterStartPolicy.resolve(
            selectedDate = today,
            today = today,
            isStepCounterAvailable = false,
            requiresActivityRecognitionPermission = true,
            hasActivityRecognitionPermission = false
        )

        assertTrue(decision is DailyStepCounterStartPolicy.Decision.SensorUnavailable)
    }

    @Test
    fun resolve_requestsPermissionWhenRequiredAndMissing() {
        val decision = DailyStepCounterStartPolicy.resolve(
            selectedDate = today,
            today = today,
            isStepCounterAvailable = true,
            requiresActivityRecognitionPermission = true,
            hasActivityRecognitionPermission = false
        )

        assertTrue(decision is DailyStepCounterStartPolicy.Decision.RequestActivityRecognitionPermission)
    }

    @Test
    fun resolve_startsCounterWhenPermissionIsGranted() {
        val decision = DailyStepCounterStartPolicy.resolve(
            selectedDate = today,
            today = today,
            isStepCounterAvailable = true,
            requiresActivityRecognitionPermission = true,
            hasActivityRecognitionPermission = true
        )

        assertTrue(decision is DailyStepCounterStartPolicy.Decision.Start)
    }

    @Test
    fun resolve_startsCounterWhenPermissionIsNotRequired() {
        val decision = DailyStepCounterStartPolicy.resolve(
            selectedDate = today,
            today = today,
            isStepCounterAvailable = true,
            requiresActivityRecognitionPermission = false,
            hasActivityRecognitionPermission = false
        )

        assertTrue(decision is DailyStepCounterStartPolicy.Decision.Start)
    }
}
