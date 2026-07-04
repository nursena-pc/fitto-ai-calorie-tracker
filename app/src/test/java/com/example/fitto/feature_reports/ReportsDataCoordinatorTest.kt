package com.example.fitto.feature_reports

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ReportsDataCoordinatorTest {

    @Test
    fun loadNutrition_forwardsResolvedDatesAndLocale() = runBlocking {
        val nutritionSource = RecordingNutritionDataSource()
        val coordinator = ReportsDataCoordinator(
            nutritionDataSource = nutritionSource,
            healthDataSource = ImmediateHealthDataSource(),
            daysBack = 2,
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            ioDispatcher = Dispatchers.Unconfined
        )

        val state = coordinator.loadNutrition(Locale.ENGLISH)

        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 4)
            ),
            nutritionSource.loadedDates
        )
        assertEquals(Locale.ENGLISH, nutritionSource.loadedLocale)
        assertEquals(nutritionSource.result, state?.nutritionDays)
    }

    @Test
    fun loadHealthData_updatesStateOnSuccess() {
        val healthData = ProfileHealthData(dailyTargetCalories = 2200)
        val coordinator = ReportsDataCoordinator(
            nutritionDataSource = RecordingNutritionDataSource(),
            healthDataSource = ImmediateHealthDataSource(healthData = healthData),
            daysBack = 0,
            initialSelectedDate = LocalDate.of(2026, 7, 4)
        )
        var receivedState: ReportsDataState? = null

        coordinator.loadHealthData { state -> receivedState = state }

        assertEquals(healthData, receivedState?.healthData)
        assertEquals(healthData, coordinator.state.healthData)
    }

    @Test
    fun loadHealthData_usesEmptyHealthDataOnFailure() {
        val coordinator = ReportsDataCoordinator(
            nutritionDataSource = RecordingNutritionDataSource(),
            healthDataSource = ImmediateHealthDataSource(shouldFail = true),
            daysBack = 0,
            initialSelectedDate = LocalDate.of(2026, 7, 4)
        )
        var receivedState: ReportsDataState? = null

        coordinator.loadHealthData { state -> receivedState = state }

        assertEquals(ProfileHealthData.Empty, receivedState?.healthData)
        assertEquals(ProfileHealthData.Empty, coordinator.state.healthData)
    }

    private class RecordingNutritionDataSource : ReportNutritionDataSource {
        var loadedDates: List<LocalDate> = emptyList()
        var loadedLocale: Locale? = null

        val result = listOf(
            ReportDay(
                date = LocalDate.of(2026, 7, 4),
                label = "Sat",
                calories = 1800,
                protein = 100.0,
                carbs = 180.0,
                fat = 60.0
            )
        )

        override suspend fun loadDays(
            dates: List<LocalDate>,
            locale: Locale
        ): List<ReportDay> {
            loadedDates = dates
            loadedLocale = locale
            return result
        }
    }

    private class ImmediateHealthDataSource(
        private val healthData: ProfileHealthData = ProfileHealthData.Empty,
        private val shouldFail: Boolean = false
    ) : ReportHealthDataSource {

        override fun load(
            onSuccess: (ProfileHealthData) -> Unit,
            onError: () -> Unit
        ) {
            if (shouldFail) {
                onError()
            } else {
                onSuccess(healthData)
            }
        }
    }
}
