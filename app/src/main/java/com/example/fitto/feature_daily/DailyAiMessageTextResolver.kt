package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_daily.ai.AiDailyTriggerType

object DailyAiMessageTextResolver {

    fun resolveCacheLocalMessageResList(triggerType: AiDailyTriggerType): List<Int> {
        return listOf(
            R.string.daily_ai_default_message,
            R.string.daily_ai_default_subtitle,
            R.string.daily_ai_loading,
            R.string.daily_ai_preparing,
            R.string.daily_ai_error_general,
            R.string.daily_ai_error_network,
            R.string.daily_ai_error_rate_limited,
            resolveLocalMessageRes(triggerType)
        )
    }

    @StringRes
    fun resolveLocalMessageRes(triggerType: AiDailyTriggerType): Int {
        return when (triggerType) {
            AiDailyTriggerType.NO_MEAL -> R.string.daily_ai_no_meal_local
            AiDailyTriggerType.CALORIE_EXCEEDED -> R.string.daily_ai_calorie_exceeded_local
            AiDailyTriggerType.CALORIE_REACHED -> R.string.daily_ai_calorie_reached_local
            AiDailyTriggerType.LOW_WATER -> R.string.daily_ai_low_water_local
            AiDailyTriggerType.LOW_PROTEIN,
            AiDailyTriggerType.LOW_DAILY_PROTEIN -> R.string.daily_ai_low_protein_local
            AiDailyTriggerType.LOW_MEAL_PROTEIN -> R.string.daily_ai_low_meal_protein_local
            AiDailyTriggerType.INSUFFICIENT_MEAL -> R.string.daily_ai_insufficient_meal_local
            AiDailyTriggerType.NO_EXERCISE -> R.string.daily_ai_no_exercise_local
            AiDailyTriggerType.NORMAL -> R.string.daily_ai_default_message
        }
    }

    @StringRes
    fun resolveErrorMessageRes(throwable: Throwable): Int {
        return resolveErrorMessageRes(DailyAiFailureResolver.resolve(throwable))
    }

    @StringRes
    fun resolveErrorMessageRes(failureKind: DailyAiFailureKind): Int {
        return when (failureKind) {
            DailyAiFailureKind.NETWORK -> R.string.daily_ai_error_network
            DailyAiFailureKind.RATE_LIMIT -> R.string.daily_ai_error_rate_limited
            DailyAiFailureKind.GENERAL -> R.string.daily_ai_error_general
        }
    }
}
