package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R

object DailyFastingTextResolver {

    data class AnalysisTextRes(
        @StringRes val rangeTitleRes: Int,
        @StringRes val bodyTextRes: Int
    )

    fun resolveAnalysisTextRes(
        range: DailyFastingPolicy.AnalysisRange
    ): AnalysisTextRes {
        return when (range) {
            DailyFastingPolicy.AnalysisRange.HOURS_0_TO_4 -> AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_0_4,
                bodyTextRes = R.string.daily_fasting_elapsed_body_0_4
            )
            DailyFastingPolicy.AnalysisRange.HOURS_4_TO_8 -> AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_4_8,
                bodyTextRes = R.string.daily_fasting_elapsed_body_4_8
            )
            DailyFastingPolicy.AnalysisRange.HOURS_8_TO_12 -> AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_8_12,
                bodyTextRes = R.string.daily_fasting_elapsed_body_8_12
            )
            DailyFastingPolicy.AnalysisRange.HOURS_12_PLUS -> AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_12_16,
                bodyTextRes = R.string.daily_fasting_elapsed_body_12_16
            )
        }
    }

    @StringRes
    fun resolveMealSuggestionRes(fastingHours: Int): Int {
        return when (DailyFastingPolicy.resolveMealSuggestion(fastingHours)) {
            DailyFastingPolicy.MealSuggestion.LONG -> {
                R.string.daily_fasting_meal_suggestion_long
            }
            DailyFastingPolicy.MealSuggestion.MEDIUM -> {
                R.string.daily_fasting_meal_suggestion_medium
            }
            DailyFastingPolicy.MealSuggestion.SHORT -> {
                R.string.daily_fasting_meal_suggestion_short
            }
        }
    }
}
