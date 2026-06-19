package com.example.fitto.feature_daily

import com.example.fitto.R
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiMessageTextResolverTest {

    @Test
    fun resolveLocalMessageRes_mapsTriggerTypesToMessageResources() {
        assertEquals(
            R.string.daily_ai_default_message,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.NORMAL)
        )
        assertEquals(
            R.string.daily_ai_no_meal_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.NO_MEAL)
        )
        assertEquals(
            R.string.daily_ai_calorie_reached_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.CALORIE_REACHED)
        )
        assertEquals(
            R.string.daily_ai_calorie_exceeded_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.CALORIE_EXCEEDED)
        )
        assertEquals(
            R.string.daily_ai_low_water_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.LOW_WATER)
        )
        assertEquals(
            R.string.daily_ai_low_protein_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.LOW_PROTEIN)
        )
        assertEquals(
            R.string.daily_ai_low_protein_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.LOW_DAILY_PROTEIN)
        )
        assertEquals(
            R.string.daily_ai_low_meal_protein_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.LOW_MEAL_PROTEIN)
        )
        assertEquals(
            R.string.daily_ai_insufficient_meal_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.INSUFFICIENT_MEAL)
        )
        assertEquals(
            R.string.daily_ai_no_exercise_local,
            DailyAiMessageTextResolver.resolveLocalMessageRes(AiDailyTriggerType.NO_EXERCISE)
        )
    }

    @Test
    fun resolveErrorMessageRes_mapsNetworkErrors() {
        assertEquals(
            R.string.daily_ai_error_network,
            DailyAiMessageTextResolver.resolveErrorMessageRes(RuntimeException("network_error"))
        )
    }

    @Test
    fun resolveErrorMessageRes_mapsRateLimitErrors() {
        assertEquals(
            R.string.daily_ai_error_rate_limited,
            DailyAiMessageTextResolver.resolveErrorMessageRes(RuntimeException("gemini_http_429"))
        )
        assertEquals(
            R.string.daily_ai_error_rate_limited,
            DailyAiMessageTextResolver.resolveErrorMessageRes(RuntimeException("HTTP 429"))
        )
    }

    @Test
    fun resolveErrorMessageRes_usesGeneralMessageForUnknownErrors() {
        assertEquals(
            R.string.daily_ai_error_general,
            DailyAiMessageTextResolver.resolveErrorMessageRes(RuntimeException("unknown"))
        )
    }
}
