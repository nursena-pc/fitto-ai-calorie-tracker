package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_daily.ai.CachedDailyAiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAiCachePolicyTest {

    @Test
    fun normalizeCacheText_trimsCollapsesWhitespaceAndLowercases() {
        val text = DailyAiCachePolicy.normalizeCacheText("  Fresh\nAI   Message  ")

        assertEquals("fresh ai message", text)
    }

    @Test
    fun requestKey_includesSnapshotDataSignature() {
        val snapshot = dailyAiSnapshot()

        val key = DailyAiCachePolicy.requestKey(snapshot)

        assertEquals(
            "2026-06-19-CALORIE_EXCEEDED-cal=2100|meals=1|water=900|protein=76|exercise=count=1|min=30|cal=220|summary=walk outside|prefs=diet=balanced|allergy=peanut|dislike=broccoli|routine=three meals",
            key
        )
    }

    @Test
    fun matchesSnapshot_acceptsMatchingCachedMessage() {
        val snapshot = dailyAiSnapshot()
        val cachedMessage = cachedMessageFor(snapshot)

        assertTrue(DailyAiCachePolicy.matchesSnapshot(cachedMessage, snapshot))
    }

    @Test
    fun matchesSnapshot_rejectsDifferentNutritionPreferences() {
        val snapshot = dailyAiSnapshot()
        val cachedMessage = cachedMessageFor(
            snapshot,
            nutritionPreferenceSignature = "diet=keto|allergy=peanut|dislike=broccoli|routine=three meals"
        )

        assertFalse(DailyAiCachePolicy.matchesSnapshot(cachedMessage, snapshot))
    }

    @Test
    fun isUsableCachedMessage_rejectsWrongSourceBlankAndLocalMessages() {
        val snapshot = dailyAiSnapshot()

        assertFalse(
            DailyAiCachePolicy.isUsableCachedMessage(
                cachedMessage = cachedMessageFor(snapshot, source = "old_cache"),
                snapshot = snapshot,
                localMessages = emptyList()
            )
        )
        assertFalse(
            DailyAiCachePolicy.isUsableCachedMessage(
                cachedMessage = cachedMessageFor(snapshot, aiMessage = "   "),
                snapshot = snapshot,
                localMessages = emptyList()
            )
        )
        assertFalse(
            DailyAiCachePolicy.isUsableCachedMessage(
                cachedMessage = cachedMessageFor(snapshot, aiMessage = "Default local message"),
                snapshot = snapshot,
                localMessages = listOf(" default   local message ")
            )
        )
    }

    @Test
    fun isUsableCachedMessage_acceptsFreshAiMessage() {
        val snapshot = dailyAiSnapshot()

        val usable = DailyAiCachePolicy.isUsableCachedMessage(
            cachedMessage = cachedMessageFor(snapshot, aiMessage = "Prioritize protein at dinner."),
            snapshot = snapshot,
            localMessages = listOf("Default local message")
        )

        assertTrue(usable)
    }

    private fun dailyAiSnapshot(): DailyAiSnapshot {
        return DailyAiSnapshot(
            dateIso = "2026-06-19",
            userName = "Nursena",
            goalType = "LOSE_WEIGHT",
            dailyCalorieTarget = 1800,
            totalCalories = 2100,
            proteinGrams = 75.6,
            proteinTargetGrams = 120,
            carbGrams = 240.0,
            fatGrams = 70.0,
            waterMl = 900,
            waterGoalMl = 2500,
            triggerType = AiDailyTriggerType.CALORIE_EXCEEDED,
            meals = listOf(
                DailyAiMealSnapshot(
                    mealTypeLabel = "Lunch",
                    totalCalories = 700,
                    proteinGrams = 32.0,
                    carbGrams = 80.0,
                    fatGrams = 25.0,
                    entryCount = 2,
                    foodSummary = "Chicken, rice"
                )
            ),
            exerciseLogCount = 1,
            exerciseDurationMinutes = 30,
            exerciseCalories = 220,
            exerciseSummaryText = " Walk\noutside ",
            dietPreferences = "Balanced",
            allergies = "Peanut",
            dislikedFoods = "Broccoli",
            mealRoutine = "Three meals"
        )
    }

    private fun cachedMessageFor(
        snapshot: DailyAiSnapshot,
        aiMessage: String = "A useful AI message",
        source: String = AiDailyAnalysisCache.CACHE_SOURCE,
        nutritionPreferenceSignature: String = DailyAiCachePolicy.nutritionPreferenceSignature(snapshot)
    ): CachedDailyAiMessage {
        return CachedDailyAiMessage(
            date = snapshot.dateIso,
            triggerType = snapshot.triggerType,
            aiMessage = aiMessage,
            createdAt = 0L,
            source = source,
            uid = "test-user",
            totalCalories = snapshot.totalCalories,
            mealCount = snapshot.meals.size,
            waterMl = snapshot.waterMl,
            proteinGrams = snapshot.proteinGrams,
            exerciseSignature = DailyAiCachePolicy.exerciseSignature(snapshot),
            nutritionPreferenceSignature = nutritionPreferenceSignature
        )
    }
}
