package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_daily.ai.CachedDailyAiMessage

interface DailyAiCacheStore {
    fun get(dateIso: String, triggerType: AiDailyTriggerType): CachedDailyAiMessage?
    fun save(snapshot: DailyAiSnapshot, aiMessage: String)
    fun clear(dateIso: String, triggerType: AiDailyTriggerType)
    fun hasAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType): Boolean
    fun markAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType)
    fun getCooldownUntil(): Long
    fun setCooldownFor(milliseconds: Long)
}

class AndroidDailyAiCacheStore(
    private val cache: AiDailyAnalysisCache
) : DailyAiCacheStore {

    override fun get(
        dateIso: String,
        triggerType: AiDailyTriggerType
    ): CachedDailyAiMessage? = cache.get(dateIso, triggerType)

    override fun save(snapshot: DailyAiSnapshot, aiMessage: String) {
        cache.save(
            dateIso = snapshot.dateIso,
            triggerType = snapshot.triggerType,
            aiMessage = aiMessage,
            totalCalories = snapshot.totalCalories,
            mealCount = snapshot.meals.size,
            waterMl = snapshot.waterMl,
            proteinGrams = snapshot.proteinGrams,
            exerciseSignature = DailyAiCachePolicy.exerciseSignature(snapshot),
            nutritionPreferenceSignature = DailyAiCachePolicy.nutritionPreferenceSignature(snapshot)
        )
    }

    override fun clear(dateIso: String, triggerType: AiDailyTriggerType) {
        cache.clear(dateIso, triggerType)
    }

    override fun hasAutoAttempted(
        dateIso: String,
        triggerType: AiDailyTriggerType
    ): Boolean = cache.hasAutoAttempted(dateIso, triggerType)

    override fun markAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType) {
        cache.markAutoAttempted(dateIso, triggerType)
    }

    override fun getCooldownUntil(): Long = cache.getCooldownUntil()

    override fun setCooldownFor(milliseconds: Long) {
        cache.setCooldownFor(milliseconds)
    }
}
