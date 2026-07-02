package com.example.fitto.feature_daily

import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWeeklyProgressLoadActionTest {

    @Test
    fun `loads oldest to selected date and calculates weekly progress`() = runBlocking {
        val requestedDates = mutableListOf<String>()
        val caloriesByDate = mapOf(
            "2026-06-29" to 500,
            "2026-06-30" to 1000,
            "2026-07-01" to 1500,
            "2026-07-02" to 2000
        )
        val action = DailyWeeklyProgressLoadAction(
            loadProfile = { testProfile(userId = "user") },
            getCaloriesForDate = { dateIso ->
                requestedDates += dateIso
                caloriesByDate[dateIso] ?: 0
            },
            resolveNutritionTargets = {
                DailyNutritionTargetResolver.Targets(2000, 120, 200, 60)
            }
        )

        val result = action.load(
            DailyWeeklyProgressLoadRequest(
                selectedDate = LocalDate.of(2026, 7, 2),
                minHeightPx = 10,
                maxHeightPx = 90,
                dayCount = 4
            )
        )

        assertEquals(
            listOf("2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02"),
            requestedDates
        )
        assertEquals(requestedDates, result.days.map(LocalDate::toString))
        assertEquals(2000, result.targetCalories)
        assertEquals(4, result.progress.daysWithFood)
        assertEquals(63, result.progress.averageProgress)
        assertEquals(listOf(25, 50, 75, 100), result.progress.days.map { it.progressPercent })
    }

    @Test
    fun `non-positive target and negative calories are normalized safely`() = runBlocking {
        val action = DailyWeeklyProgressLoadAction(
            loadProfile = { null },
            getCaloriesForDate = { -200 },
            resolveNutritionTargets = {
                DailyNutritionTargetResolver.Targets(0, 120, 200, 60)
            }
        )

        val result = action.load(
            DailyWeeklyProgressLoadRequest(
                selectedDate = LocalDate.of(2026, 7, 2),
                minHeightPx = 18,
                maxHeightPx = 90,
                dayCount = 2
            )
        )

        assertEquals(1, result.targetCalories)
        assertEquals(0, result.progress.daysWithFood)
        assertTrue(result.progress.days.all { day -> day.calories == 0 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `day count must be positive`() {
        DailyWeeklyProgressLoadRequest(
            selectedDate = LocalDate.of(2026, 7, 2),
            minHeightPx = 18,
            maxHeightPx = 90,
            dayCount = 0
        )
    }
}
