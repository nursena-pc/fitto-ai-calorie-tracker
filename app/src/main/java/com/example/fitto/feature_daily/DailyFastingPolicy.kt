package com.example.fitto.feature_daily

import java.util.Locale

object DailyFastingPolicy {

    enum class AnalysisRange {
        HOURS_0_TO_4,
        HOURS_4_TO_8,
        HOURS_8_TO_12,
        HOURS_12_PLUS
    }

    enum class MealSuggestion {
        SHORT,
        MEDIUM,
        LONG
    }

    fun resolveFastingHours(
        fastingTypeName: String?,
        customFastingHours: Int?
    ): Int {
        val normalizedName = fastingTypeName
            .orEmpty()
            .lowercase(Locale.ROOT)

        return when {
            normalizedName.contains("12") -> 12
            normalizedName.contains("15") -> 15
            normalizedName.contains("16") -> 16
            normalizedName.contains("custom") -> customFastingHours
                ?.coerceIn(MIN_CUSTOM_FASTING_HOURS, MAX_CUSTOM_FASTING_HOURS)
                ?: DEFAULT_FASTING_HOURS
            else -> DEFAULT_FASTING_HOURS
        }
    }

    fun resolveMealSuggestion(fastingHours: Int): MealSuggestion {
        return when {
            fastingHours >= LONG_FASTING_HOURS -> MealSuggestion.LONG
            fastingHours >= MEDIUM_FASTING_HOURS -> MealSuggestion.MEDIUM
            else -> MealSuggestion.SHORT
        }
    }

    fun calculateElapsedHours(
        fastingHours: Int,
        remainingMillis: Long
    ): Long {
        val totalDurationMillis = fastingHours * MILLIS_PER_HOUR
        val elapsedMillis = (totalDurationMillis - remainingMillis).coerceAtLeast(0L)

        return elapsedMillis / MILLIS_PER_HOUR
    }

    fun resolveAnalysisRange(elapsedHours: Long): AnalysisRange {
        return when {
            elapsedHours < 4L -> AnalysisRange.HOURS_0_TO_4
            elapsedHours < 8L -> AnalysisRange.HOURS_4_TO_8
            elapsedHours < 12L -> AnalysisRange.HOURS_8_TO_12
            else -> AnalysisRange.HOURS_12_PLUS
        }
    }

    private const val DEFAULT_FASTING_HOURS = 16
    private const val MIN_CUSTOM_FASTING_HOURS = 8
    private const val MAX_CUSTOM_FASTING_HOURS = 24
    private const val MEDIUM_FASTING_HOURS = 14
    private const val LONG_FASTING_HOURS = 16
    private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
}
