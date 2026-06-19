package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_daily.ai.GeminiDailyAiRepository

object DailyAiMessageTextResolver {

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
        val message = throwable.message.orEmpty()
        return when {
            message.contains(GeminiDailyAiRepository.ERROR_NETWORK) -> {
                R.string.daily_ai_error_network
            }
            message.contains(GeminiDailyAiRepository.ERROR_RATE_LIMIT) ||
                message.contains("429") -> {
                R.string.daily_ai_error_rate_limited
            }
            else -> R.string.daily_ai_error_general
        }
    }
}
