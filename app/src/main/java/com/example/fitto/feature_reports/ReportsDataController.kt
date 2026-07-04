package com.example.fitto.feature_reports

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import java.time.LocalDate

internal data class ReportsDataState(
    val selectedDate: LocalDate,
    val nutritionDays: List<ReportDay> = emptyList(),
    val healthData: ProfileHealthData = ProfileHealthData.Empty
)

internal data class ReportNutritionRequest(
    val id: Long,
    val selectedDate: LocalDate,
    val dates: List<LocalDate>
)

internal class ReportsDataController(
    initialSelectedDate: LocalDate,
    private val daysBack: Int
) {

    init {
        require(daysBack >= 0) { "daysBack must not be negative" }
    }

    private var nextNutritionRequestId = 0L
    private var activeNutritionRequestId = 0L
    private var currentState = ReportsDataState(selectedDate = initialSelectedDate)

    val state: ReportsDataState
        get() = currentState

    fun selectDate(date: LocalDate): ReportsDataState {
        currentState = currentState.copy(selectedDate = date)
        return currentState
    }

    fun createNutritionRequest(): ReportNutritionRequest {
        val requestId = ++nextNutritionRequestId
        activeNutritionRequestId = requestId

        return ReportNutritionRequest(
            id = requestId,
            selectedDate = currentState.selectedDate,
            dates = (daysBack downTo 0).map { offset ->
                currentState.selectedDate.minusDays(offset.toLong())
            }
        )
    }

    fun acceptNutrition(
        request: ReportNutritionRequest,
        days: List<ReportDay>
    ): ReportsDataState? {
        val isCurrentRequest = request.id == activeNutritionRequestId &&
            request.selectedDate == currentState.selectedDate

        if (!isCurrentRequest) return null

        currentState = currentState.copy(nutritionDays = days)
        return currentState
    }

    fun acceptHealthData(healthData: ProfileHealthData): ReportsDataState {
        currentState = currentState.copy(healthData = healthData)
        return currentState
    }

    fun acceptHealthDataFailure(): ReportsDataState {
        currentState = currentState.copy(healthData = ProfileHealthData.Empty)
        return currentState
    }
}
