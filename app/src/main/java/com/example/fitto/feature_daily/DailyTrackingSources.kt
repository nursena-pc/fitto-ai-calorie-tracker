package com.example.fitto.feature_daily

import com.example.fitto.feature_health.HealthConnectDailySummary
import com.example.fitto.feature_sleep.data.SleepLogLocalModel
import java.time.LocalDate

/** Platform tracking sources consumed by the Daily dashboard action graph. */
internal data class DailyTrackingSources(
    val readHealthConnectSummary: suspend (LocalDate) -> HealthConnectDailySummary,
    val getCachedSteps: (LocalDate) -> Long,
    val isStepCounterAvailable: () -> Boolean,
    val startStepCounter: (LocalDate, (Long) -> Unit) -> Unit,
    val stopStepCounter: () -> Unit,
    val getSleepLogForDate: (String) -> SleepLogLocalModel?,
    val saveSleepLog: (SleepLogLocalModel) -> Unit
)
