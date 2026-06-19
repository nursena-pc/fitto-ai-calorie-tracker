package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyWeeklyCalorieProgressCalculatorTest {

    @Test
    fun calculate_withoutFood_returnsEmptyProgress() {
        val summary = DailyWeeklyCalorieProgressCalculator.calculate(
            dailyCalories = listOf(0, -100, 0),
            targetCalories = 1800,
            minHeightPx = 18,
            maxHeightPx = 90
        )

        assertEquals(0, summary.daysWithFood)
        assertEquals(0, summary.averageProgress)
        assertEquals(0, summary.progressBarPercent)
        assertEquals(listOf(18, 18, 18), summary.days.map { it.heightPx })
        assertEquals(listOf(0, 0, 0), summary.days.map { it.progressPercent })
        assertEquals(listOf(0.25f, 0.25f, 0.25f), summary.days.map { it.alpha })
    }

    @Test
    fun calculate_withFood_averagesOnlyFoodDaysAndCapsVisualProgress() {
        val summary = DailyWeeklyCalorieProgressCalculator.calculate(
            dailyCalories = listOf(0, 500, 1000, 1500, 2000),
            targetCalories = 1000,
            minHeightPx = 18,
            maxHeightPx = 90
        )

        assertEquals(4, summary.daysWithFood)
        assertEquals(113, summary.averageProgress)
        assertEquals(100, summary.progressBarPercent)
        assertEquals(listOf(0, 50, 100, 150, 150), summary.days.map { it.progressPercent })
        assertEquals(listOf(18, 54, 90, 90, 90), summary.days.map { it.heightPx })
        assertEquals(listOf(false, true, true, true, true), summary.days.map { it.hasFood })
    }

    @Test
    fun calculate_withInvalidTarget_usesSafeMinimumTarget() {
        val summary = DailyWeeklyCalorieProgressCalculator.calculate(
            dailyCalories = listOf(2),
            targetCalories = 0,
            minHeightPx = 18,
            maxHeightPx = 90
        )

        assertEquals(1, summary.daysWithFood)
        assertEquals(100, summary.progressBarPercent)
        assertEquals(90, summary.days.first().heightPx)
        assertEquals(150, summary.days.first().progressPercent)
    }
}
