package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.GeminiDailyAiRepository

enum class DailyAiFailureKind {
    NETWORK,
    RATE_LIMIT,
    GENERAL
}

object DailyAiFailureResolver {

    fun resolve(throwable: Throwable): DailyAiFailureKind {
        val message = throwable.message.orEmpty()
        return when {
            message.contains(GeminiDailyAiRepository.ERROR_NETWORK) -> {
                DailyAiFailureKind.NETWORK
            }

            message.contains(GeminiDailyAiRepository.ERROR_RATE_LIMIT) ||
                message.contains("429") -> {
                DailyAiFailureKind.RATE_LIMIT
            }

            else -> DailyAiFailureKind.GENERAL
        }
    }
}
