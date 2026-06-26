package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyFastingPolicyTest {

    @Test
    fun resolveFastingHours_usesPresetHoursFromTypeName() {
        assertEquals(
            12,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "FASTING_12_12",
                customFastingHours = 20
            )
        )
        assertEquals(
            15,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "fasting_15_9",
                customFastingHours = 20
            )
        )
        assertEquals(
            16,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "FASTING_16_8",
                customFastingHours = 20
            )
        )
    }

    @Test
    fun resolveFastingHours_customValueIsClamped() {
        assertEquals(
            8,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "CUSTOM",
                customFastingHours = 6
            )
        )
        assertEquals(
            24,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "CUSTOM",
                customFastingHours = 30
            )
        )
        assertEquals(
            18,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "CUSTOM",
                customFastingHours = 18
            )
        )
    }

    @Test
    fun resolveFastingHours_defaultsToSixteenHours() {
        assertEquals(
            16,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = null,
                customFastingHours = null
            )
        )
        assertEquals(
            16,
            DailyFastingPolicy.resolveFastingHours(
                fastingTypeName = "CUSTOM",
                customFastingHours = null
            )
        )
    }

    @Test
    fun resolveMealSuggestion_usesFastingDurationThresholds() {
        assertEquals(
            DailyFastingPolicy.MealSuggestion.SHORT,
            DailyFastingPolicy.resolveMealSuggestion(13)
        )
        assertEquals(
            DailyFastingPolicy.MealSuggestion.MEDIUM,
            DailyFastingPolicy.resolveMealSuggestion(14)
        )
        assertEquals(
            DailyFastingPolicy.MealSuggestion.LONG,
            DailyFastingPolicy.resolveMealSuggestion(16)
        )
    }

    @Test
    fun calculateElapsedHours_usesRemainingTimeAndClampsNegativeElapsedTime() {
        assertEquals(
            4L,
            DailyFastingPolicy.calculateElapsedHours(
                fastingHours = 16,
                remainingMillis = 12L * 60L * 60L * 1000L
            )
        )
        assertEquals(
            0L,
            DailyFastingPolicy.calculateElapsedHours(
                fastingHours = 16,
                remainingMillis = 20L * 60L * 60L * 1000L
            )
        )
    }

    @Test
    fun resolveAnalysisRange_usesElapsedHourBoundaries() {
        assertEquals(
            DailyFastingPolicy.AnalysisRange.HOURS_0_TO_4,
            DailyFastingPolicy.resolveAnalysisRange(3)
        )
        assertEquals(
            DailyFastingPolicy.AnalysisRange.HOURS_4_TO_8,
            DailyFastingPolicy.resolveAnalysisRange(4)
        )
        assertEquals(
            DailyFastingPolicy.AnalysisRange.HOURS_8_TO_12,
            DailyFastingPolicy.resolveAnalysisRange(8)
        )
        assertEquals(
            DailyFastingPolicy.AnalysisRange.HOURS_12_PLUS,
            DailyFastingPolicy.resolveAnalysisRange(12)
        )
    }
}
