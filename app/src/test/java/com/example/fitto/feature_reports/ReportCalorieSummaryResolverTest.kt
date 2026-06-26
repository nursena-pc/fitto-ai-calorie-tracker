package com.example.fitto.feature_reports

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportCalorieSummaryResolverTest {

    @Test
    fun resolve_usesProfileTargetWhenAvailable() {
        val state = ReportCalorieSummaryResolver.resolve(
            days = listOf(
                reportDay(calories = 1200),
                reportDay(calories = 1800),
                reportDay(calories = 2100)
            ),
            profileTargetCalories = 1900,
            defaultTargetCalories = 1800
        )

        assertEquals(1900, state.targetCalories)
        assertEquals(2100, state.todayCalories)
        assertEquals(1700, state.averageCalories)
        assertEquals(2100, state.maxCalories)
    }

    @Test
    fun resolve_usesDefaultTargetAndZeroValuesForEmptyDays() {
        val state = ReportCalorieSummaryResolver.resolve(
            days = emptyList(),
            profileTargetCalories = 0,
            defaultTargetCalories = 1800
        )

        assertEquals(1800, state.targetCalories)
        assertEquals(0, state.todayCalories)
        assertEquals(0, state.averageCalories)
        assertEquals(0, state.maxCalories)
    }

    private fun reportDay(calories: Int): ReportDay {
        return ReportDay(
            date = LocalDate.of(2026, 6, 25),
            label = "Thu",
            calories = calories,
            protein = 0.0,
            carbs = 0.0,
            fat = 0.0
        )
    }
}
