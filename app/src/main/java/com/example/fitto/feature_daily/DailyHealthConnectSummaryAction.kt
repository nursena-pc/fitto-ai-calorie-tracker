package com.example.fitto.feature_daily

import com.example.fitto.feature_health.HealthConnectDailySummary
import java.time.LocalDate

class DailyHealthConnectSummaryAction(
    private val readDailySummary: suspend (LocalDate) -> HealthConnectDailySummary
) {

    suspend fun load(date: LocalDate): DailyHealthConnectSummaryResult {
        return DailyHealthConnectSummaryResult(
            requestedDate = date,
            summary = readDailySummary(date)
        )
    }
}

data class DailyHealthConnectSummaryResult(
    val requestedDate: LocalDate,
    val summary: HealthConnectDailySummary
)
