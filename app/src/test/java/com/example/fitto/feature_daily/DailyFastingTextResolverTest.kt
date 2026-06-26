package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyFastingTextResolverTest {

    @Test
    fun resolveAnalysisTextRes_mapsElapsedRangesToTextResources() {
        assertEquals(
            DailyFastingTextResolver.AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_0_4,
                bodyTextRes = R.string.daily_fasting_elapsed_body_0_4
            ),
            DailyFastingTextResolver.resolveAnalysisTextRes(
                DailyFastingPolicy.AnalysisRange.HOURS_0_TO_4
            )
        )
        assertEquals(
            DailyFastingTextResolver.AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_4_8,
                bodyTextRes = R.string.daily_fasting_elapsed_body_4_8
            ),
            DailyFastingTextResolver.resolveAnalysisTextRes(
                DailyFastingPolicy.AnalysisRange.HOURS_4_TO_8
            )
        )
        assertEquals(
            DailyFastingTextResolver.AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_8_12,
                bodyTextRes = R.string.daily_fasting_elapsed_body_8_12
            ),
            DailyFastingTextResolver.resolveAnalysisTextRes(
                DailyFastingPolicy.AnalysisRange.HOURS_8_TO_12
            )
        )
        assertEquals(
            DailyFastingTextResolver.AnalysisTextRes(
                rangeTitleRes = R.string.daily_fasting_elapsed_range_12_16,
                bodyTextRes = R.string.daily_fasting_elapsed_body_12_16
            ),
            DailyFastingTextResolver.resolveAnalysisTextRes(
                DailyFastingPolicy.AnalysisRange.HOURS_12_PLUS
            )
        )
    }

    @Test
    fun resolveMealSuggestionRes_mapsFastingDurationsToTextResources() {
        assertEquals(
            R.string.daily_fasting_meal_suggestion_short,
            DailyFastingTextResolver.resolveMealSuggestionRes(13)
        )
        assertEquals(
            R.string.daily_fasting_meal_suggestion_medium,
            DailyFastingTextResolver.resolveMealSuggestionRes(14)
        )
        assertEquals(
            R.string.daily_fasting_meal_suggestion_long,
            DailyFastingTextResolver.resolveMealSuggestionRes(16)
        )
    }
}
