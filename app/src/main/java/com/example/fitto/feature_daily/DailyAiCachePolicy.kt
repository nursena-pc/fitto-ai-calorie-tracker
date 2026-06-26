package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.CachedDailyAiMessage
import java.util.Locale
import kotlin.math.roundToInt

object DailyAiCachePolicy {

    fun isUsableCachedMessage(
        cachedMessage: CachedDailyAiMessage,
        snapshot: DailyAiSnapshot,
        localMessages: List<String>
    ): Boolean {
        if (cachedMessage.source != AiDailyAnalysisCache.CACHE_SOURCE) return false
        if (!matchesSnapshot(cachedMessage, snapshot)) return false

        val normalizedMessage = normalizeCacheText(cachedMessage.aiMessage)
        if (normalizedMessage.isBlank()) return false

        return localMessages.none { localMessage ->
            val normalizedLocalMessage = normalizeCacheText(localMessage)
            normalizedLocalMessage.isNotBlank() && normalizedMessage == normalizedLocalMessage
        }
    }

    fun matchesSnapshot(
        cachedMessage: CachedDailyAiMessage,
        snapshot: DailyAiSnapshot
    ): Boolean {
        return cachedMessage.totalCalories == snapshot.totalCalories &&
            cachedMessage.mealCount == snapshot.meals.size &&
            cachedMessage.waterMl == snapshot.waterMl &&
            cachedMessage.proteinGrams.roundToInt() == snapshot.proteinGrams.roundToInt() &&
            cachedMessage.exerciseSignature == exerciseSignature(snapshot) &&
            cachedMessage.nutritionPreferenceSignature == nutritionPreferenceSignature(snapshot)
    }

    fun requestKey(snapshot: DailyAiSnapshot): String {
        return "${snapshot.dateIso}-${snapshot.triggerType.name}-${dataSignature(snapshot)}"
    }

    fun dataSignature(snapshot: DailyAiSnapshot): String {
        return "cal=${snapshot.totalCalories}|" +
            "meals=${snapshot.meals.size}|" +
            "water=${snapshot.waterMl}|" +
            "protein=${snapshot.proteinGrams.roundToInt()}|" +
            "exercise=${exerciseSignature(snapshot)}|" +
            "prefs=${nutritionPreferenceSignature(snapshot)}"
    }

    fun exerciseSignature(snapshot: DailyAiSnapshot): String {
        return "count=${snapshot.exerciseLogCount}|" +
            "min=${snapshot.exerciseDurationMinutes}|" +
            "cal=${snapshot.exerciseCalories}|" +
            "summary=${normalizeCacheText(snapshot.exerciseSummaryText)}"
    }

    fun nutritionPreferenceSignature(snapshot: DailyAiSnapshot): String {
        return "diet=${normalizeCacheText(snapshot.dietPreferences)}|" +
            "allergy=${normalizeCacheText(snapshot.allergies)}|" +
            "dislike=${normalizeCacheText(snapshot.dislikedFoods)}|" +
            "routine=${normalizeCacheText(snapshot.mealRoutine)}"
    }

    fun snapshotDebugText(snapshot: DailyAiSnapshot): String {
        return dataSignature(snapshot)
    }

    fun cachedMessageDebugText(cachedMessage: CachedDailyAiMessage): String {
        return "cal=${cachedMessage.totalCalories}|" +
            "meals=${cachedMessage.mealCount}|" +
            "water=${cachedMessage.waterMl}|" +
            "protein=${cachedMessage.proteinGrams.roundToInt()}|" +
            "exercise=${cachedMessage.exerciseSignature}|" +
            "prefs=${cachedMessage.nutritionPreferenceSignature}"
    }

    fun normalizeCacheText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }
}
