package com.example.fitto.feature_reports

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportsDataControllerTest {

    @Test
    fun createNutritionRequest_buildsRangeEndingOnSelectedDate() {
        val controller = ReportsDataController(
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            daysBack = 6
        )

        val request = controller.createNutritionRequest()

        assertEquals(LocalDate.of(2026, 7, 4), request.selectedDate)
        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 28),
                LocalDate.of(2026, 6, 29),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 4)
            ),
            request.dates
        )
    }

    @Test
    fun acceptNutrition_updatesStateForCurrentRequest() {
        val controller = ReportsDataController(
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            daysBack = 1
        )
        val request = controller.createNutritionRequest()
        val days = listOf(reportDay(LocalDate.of(2026, 7, 4), calories = 1800))

        val state = controller.acceptNutrition(request, days)

        assertEquals(days, state?.nutritionDays)
        assertEquals(days, controller.state.nutritionDays)
    }

    @Test
    fun acceptNutrition_ignoresResponseForPreviouslySelectedDate() {
        val controller = ReportsDataController(
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            daysBack = 1
        )
        val oldRequest = controller.createNutritionRequest()

        controller.selectDate(LocalDate.of(2026, 7, 5))
        controller.createNutritionRequest()

        val state = controller.acceptNutrition(
            oldRequest,
            listOf(reportDay(LocalDate.of(2026, 7, 4), calories = 1800))
        )

        assertNull(state)
        assertEquals(emptyList<ReportDay>(), controller.state.nutritionDays)
    }

    @Test
    fun acceptNutrition_ignoresOlderRequestForSameDate() {
        val controller = ReportsDataController(
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            daysBack = 1
        )
        val olderRequest = controller.createNutritionRequest()
        controller.createNutritionRequest()

        val state = controller.acceptNutrition(
            olderRequest,
            listOf(reportDay(LocalDate.of(2026, 7, 4), calories = 1800))
        )

        assertNull(state)
        assertEquals(emptyList<ReportDay>(), controller.state.nutritionDays)
    }

    @Test
    fun healthUpdates_preserveSelectedDateAndNutritionDays() {
        val controller = ReportsDataController(
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            daysBack = 0
        )
        val request = controller.createNutritionRequest()
        val days = listOf(reportDay(LocalDate.of(2026, 7, 4), calories = 1800))
        controller.acceptNutrition(request, days)
        val healthData = ProfileHealthData(dailyTargetCalories = 2100)

        val loadedState = controller.acceptHealthData(healthData)
        val failedState = controller.acceptHealthDataFailure()

        assertEquals(days, loadedState.nutritionDays)
        assertEquals(healthData, loadedState.healthData)
        assertEquals(LocalDate.of(2026, 7, 4), loadedState.selectedDate)
        assertEquals(days, failedState.nutritionDays)
        assertEquals(ProfileHealthData.Empty, failedState.healthData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeDaysBack_isRejected() {
        ReportsDataController(
            initialSelectedDate = LocalDate.of(2026, 7, 4),
            daysBack = -1
        )
    }

    private fun reportDay(
        date: LocalDate,
        calories: Int
    ): ReportDay {
        return ReportDay(
            date = date,
            label = "Sat",
            calories = calories,
            protein = 10.0,
            carbs = 20.0,
            fat = 5.0
        )
    }
}
