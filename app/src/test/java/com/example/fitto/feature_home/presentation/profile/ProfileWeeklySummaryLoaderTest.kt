package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.data.local.model.DailyNutritionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProfileWeeklySummaryLoaderTest {

    @Test
    fun load_buildsSevenDaySummaryEndingToday() = runBlocking {
        val calls = mutableListOf<Pair<String, String>>()
        val summaries = (1..7).associate { day ->
            LocalDate.of(2026, 7, day).toString() to DailyNutritionSummary(
                totalCalories = day * 100,
                totalProtein = day.toDouble(),
                totalCarbs = day * 2.0,
                totalFat = day * 0.5
            )
        }
        val loader = ProfileWeeklySummaryLoader(
            activeUserId = { "user-1" },
            nutritionSource = ProfileNutritionSummarySource { userId, dateIso ->
                calls += userId to dateIso
                summaries.getValue(dateIso)
            },
            ioDispatcher = Dispatchers.Unconfined,
            today = { LocalDate.of(2026, 7, 7) }
        )

        val result = loader.load()

        assertEquals(7, calls.size)
        assertEquals("user-1" to "2026-07-01", calls.first())
        assertEquals("user-1" to "2026-07-07", calls.last())
        assertEquals(700, result.todayCalories)
        assertEquals(2800, result.weeklyTotalCalories)
        assertEquals(400, result.weeklyAverageCalories)
        assertEquals(7, result.recordedDays)
        assertEquals(28.0, result.weeklyProtein, 0.0)
        assertEquals(56.0, result.weeklyCarbs, 0.0)
        assertEquals(14.0, result.weeklyFat, 0.0)
    }

    @Test
    fun load_usesAnonymousScopeAndZeroAverageWhenNoDayHasData() = runBlocking {
        val users = mutableListOf<String>()
        val loader = ProfileWeeklySummaryLoader(
            activeUserId = { null },
            nutritionSource = ProfileNutritionSummarySource { userId, _ ->
                users += userId
                DailyNutritionSummary(0, 0.0, 0.0, 0.0)
            },
            ioDispatcher = Dispatchers.Unconfined,
            today = { LocalDate.of(2026, 7, 7) }
        )

        val result = loader.load()

        assertEquals(setOf("local_anonymous"), users.toSet())
        assertEquals(0, result.recordedDays)
        assertEquals(0, result.weeklyAverageCalories)
    }
}
