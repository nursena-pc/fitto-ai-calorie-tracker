package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyMetricCalculatorTest {

    @Test
    fun sleepDuration_crossingMidnight_returnsNextDayDuration() {
        val durationMinutes = DailyMetricCalculator.calculateSleepDurationMinutes(
            bedtimeHour = 23,
            bedtimeMinute = 0,
            wakeHour = 7,
            wakeMinute = 0
        )

        assertEquals(480, durationMinutes)
    }

    @Test
    fun sleepCalories_usesSleepMetAndWeight() {
        val calories = DailyMetricCalculator.calculateSleepCalories(
            durationMinutes = 480,
            weightKg = 70.0
        )

        assertEquals(532, calories)
    }

    @Test
    fun waterHelpers_alignValuesToBottleSize() {
        assertEquals(750, DailyMetricCalculator.roundUpToBottleSize(701))
        assertEquals(500, DailyMetricCalculator.normalizeWaterToBottleSize(749))
    }

    @Test
    fun progressPercent_clampsToValidProgressRange() {
        assertEquals(0, DailyMetricCalculator.progressPercent(current = 50.0, target = 0.0))
        assertEquals(50, DailyMetricCalculator.progressPercent(current = 50.0, target = 100.0))
        assertEquals(100, DailyMetricCalculator.progressPercent(current = 150.0, target = 100.0))
    }
}
