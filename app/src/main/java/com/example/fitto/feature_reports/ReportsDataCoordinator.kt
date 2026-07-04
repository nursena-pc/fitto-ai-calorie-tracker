package com.example.fitto.feature_reports

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

internal class ReportsDataCoordinator(
    private val nutritionDataSource: ReportNutritionDataSource,
    private val healthDataSource: ReportHealthDataSource,
    daysBack: Int,
    initialSelectedDate: LocalDate = LocalDate.now(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val controller = ReportsDataController(
        initialSelectedDate = initialSelectedDate,
        daysBack = daysBack
    )

    val state: ReportsDataState
        get() = controller.state

    fun selectDate(date: LocalDate): ReportsDataState {
        return controller.selectDate(date)
    }

    suspend fun loadNutrition(locale: Locale): ReportsDataState? {
        val request = controller.createNutritionRequest()
        val days = withContext(ioDispatcher) {
            nutritionDataSource.loadDays(
                dates = request.dates,
                locale = locale
            )
        }

        return controller.acceptNutrition(request, days)
    }

    fun loadHealthData(onStateChanged: (ReportsDataState) -> Unit) {
        healthDataSource.load(
            onSuccess = { healthData ->
                onStateChanged(controller.acceptHealthData(healthData))
            },
            onError = {
                onStateChanged(controller.acceptHealthDataFailure())
            }
        )
    }
}
