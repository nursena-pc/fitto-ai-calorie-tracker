package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_daily.ai.CachedDailyAiMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyAiAnalysisActionTest {

    @Test
    fun `card click without snapshot prepares a manual request after snapshot resolution`() {
        val action = createAction()

        assertEquals(
            DailyAiAnalysisAction.CardClickDecision.PrepareSnapshot,
            action.onCardClicked(null)
        )

        val snapshot = snapshot()
        val resolution = action.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = LocalDate.parse(snapshot.dateIso),
            today = LocalDate.parse(snapshot.dateIso)
        )

        assertEquals(DailyAiAnalysisAction.SnapshotPresentation.NoChange, resolution.presentation)
        assertEquals(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = false),
            resolution.request
        )
    }

    @Test
    fun `fresh cached message is rendered without a request`() {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore(cachedMessage = cachedMessage(snapshot, "Fresh advice"))
        val action = createAction(cache = cache)

        val resolution = action.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = LocalDate.parse(snapshot.dateIso),
            today = LocalDate.parse(snapshot.dateIso)
        )

        assertEquals(
            DailyAiAnalysisAction.SnapshotPresentation.Message(
                message = "Fresh advice",
                subtitle = DailyAiAnalysisAction.MessageSubtitle.RESULT,
                status = AiDailyStatusLevel.RED
            ),
            resolution.presentation
        )
        assertNull(resolution.request)
        assertFalse(cache.wasCleared)
    }

    @Test
    fun `stale cached message is cleared and local auto request is resolved`() {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore(
            cachedMessage = cachedMessage(snapshot, "local-message")
        )
        val action = createAction(cache = cache)

        val resolution = action.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = LocalDate.parse(snapshot.dateIso),
            today = LocalDate.parse(snapshot.dateIso)
        )

        assertEquals(
            DailyAiAnalysisAction.SnapshotPresentation.Message(
                message = "local-message",
                subtitle = DailyAiAnalysisAction.MessageSubtitle.DEFAULT,
                status = AiDailyStatusLevel.RED
            ),
            resolution.presentation
        )
        assertEquals(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = true),
            resolution.request
        )
        assertTrue(cache.wasCleared)
    }

    @Test
    fun `auto request preparation marks attempt and enters loading state`() {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore()
        val action = createAction(cache = cache)

        val preparation = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = true)
        )

        assertTrue(preparation is DailyAiAnalysisAction.RequestPreparation.Start)
        assertTrue(cache.autoAttempted)
        assertTrue(action.isLoading)
    }

    @Test
    fun `active cooldown blocks request with local fallback`() {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore(cooldownUntilValue = 3_000L)
        val action = createAction(cache = cache, nowMillis = { 1_000L })

        val preparation = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = false)
        )

        assertEquals(
            DailyAiAnalysisAction.RequestPreparation.BlockedByCooldown(
                localMessage = "local-message",
                status = AiDailyStatusLevel.RED,
                remainingSeconds = 2L
            ),
            preparation
        )
        assertFalse(action.isLoading)
    }

    @Test
    fun `second request is rejected while another request is prepared`() {
        val action = createAction()
        val first = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(snapshot(), isAuto = false)
        )
        assertTrue(first is DailyAiAnalysisAction.RequestPreparation.Start)

        val second = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(
                snapshot(dateIso = "2026-06-20"),
                isAuto = false
            )
        )

        assertEquals(
            DailyAiAnalysisAction.RequestPreparation.AlreadyLoading(AiDailyStatusLevel.RED),
            second
        )
    }

    @Test
    fun `successful request caches structured message and returns analysis`() = runBlocking {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore()
        val analysis = analysis()
        val action = createAction(
            cache = cache,
            request = { prompt, language ->
                assertEquals("prompt-tr", prompt)
                assertEquals("tr", language)
                analysis
            }
        )
        action.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = LocalDate.parse(snapshot.dateIso),
            today = LocalDate.parse(snapshot.dateIso)
        )
        val preparation = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = false)
        ) as DailyAiAnalysisAction.RequestPreparation.Start

        val result = action.execute(preparation.request)

        assertEquals(
            DailyAiAnalysisAction.RequestResult.Success(analysis, snapshot),
            result
        )
        assertEquals(snapshot, cache.savedSnapshot)
        assertEquals("structured-message", cache.savedMessage)
        assertFalse(action.isLoading)
    }

    @Test
    fun `blank structured message caches local fallback`() = runBlocking {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore()
        val action = createAction(
            cache = cache,
            structuredMessage = { "   " }
        )
        action.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = LocalDate.parse(snapshot.dateIso),
            today = LocalDate.parse(snapshot.dateIso)
        )
        val preparation = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = false)
        ) as DailyAiAnalysisAction.RequestPreparation.Start

        action.execute(preparation.request)

        assertEquals("local-message", cache.savedMessage)
    }

    @Test
    fun `rate limit failure starts cooldown and returns local fallback`() = runBlocking {
        val snapshot = snapshot()
        val cache = FakeDailyAiCacheStore()
        val action = createAction(
            cache = cache,
            request = { _, _ -> error("gemini_http_429") }
        )
        action.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = LocalDate.parse(snapshot.dateIso),
            today = LocalDate.parse(snapshot.dateIso)
        )
        val preparation = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(snapshot, isAuto = false)
        ) as DailyAiAnalysisAction.RequestPreparation.Start

        val result = action.execute(preparation.request)

        assertEquals(
            DailyAiAnalysisAction.RequestResult.Failure(
                snapshot = snapshot,
                localMessage = "local-message",
                failureKind = DailyAiFailureKind.RATE_LIMIT
            ),
            result
        )
        assertEquals(60_000L, cache.cooldownDuration)
        assertFalse(action.isLoading)
    }

    @Test
    fun `result is ignored when active snapshot changes during request`() = runBlocking {
        val firstSnapshot = snapshot()
        val secondSnapshot = snapshot(dateIso = "2026-06-20")
        lateinit var action: DailyAiAnalysisAction
        action = createAction(
            request = { _, _ ->
                action.resolveSnapshot(
                    snapshot = secondSnapshot,
                    selectedDate = LocalDate.parse(secondSnapshot.dateIso),
                    today = LocalDate.parse(secondSnapshot.dateIso)
                )
                analysis()
            }
        )
        action.resolveSnapshot(
            snapshot = firstSnapshot,
            selectedDate = LocalDate.parse(firstSnapshot.dateIso),
            today = LocalDate.parse(firstSnapshot.dateIso)
        )
        val preparation = action.prepareRequest(
            DailyAiAnalysisAction.RequestIntent(firstSnapshot, isAuto = false)
        ) as DailyAiAnalysisAction.RequestPreparation.Start

        assertEquals(
            DailyAiAnalysisAction.RequestResult.Stale,
            action.execute(preparation.request)
        )
        assertFalse(action.isLoading)
    }

    private fun createAction(
        cache: FakeDailyAiCacheStore = FakeDailyAiCacheStore(),
        request: suspend (String, String) -> AiDailyAnalysisResult = { _, _ -> analysis() },
        nowMillis: () -> Long = { 1_000L },
        structuredMessage: (AiDailyAnalysisResult) -> String = { "structured-message" }
    ): DailyAiAnalysisAction {
        return DailyAiAnalysisAction(
            cacheStore = cache,
            requestDailyAnalysis = request,
            buildPrompt = { _, language -> "prompt-$language" },
            currentLanguage = { "tr" },
            buildStructuredMessage = structuredMessage,
            resolveLocalMessage = { "local-message" },
            resolveCacheLocalMessages = { listOf("local-message") },
            nowMillis = nowMillis
        )
    }

    private fun snapshot(
        dateIso: String = "2026-06-19"
    ): DailyAiSnapshot {
        return DailyAiSnapshot(
            dateIso = dateIso,
            userName = "Nursena",
            goalType = "LOSE_WEIGHT",
            dailyCalorieTarget = 1800,
            totalCalories = 2100,
            proteinGrams = 76.0,
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
            exerciseSummaryText = "Walk",
            dietPreferences = "Balanced",
            allergies = "Peanut",
            dislikedFoods = "Broccoli",
            mealRoutine = "Three meals"
        )
    }

    private fun cachedMessage(
        snapshot: DailyAiSnapshot,
        message: String
    ): CachedDailyAiMessage {
        return CachedDailyAiMessage(
            date = snapshot.dateIso,
            triggerType = snapshot.triggerType,
            aiMessage = message,
            createdAt = 0L,
            source = AiDailyAnalysisCache.CACHE_SOURCE,
            uid = "test-user",
            totalCalories = snapshot.totalCalories,
            mealCount = snapshot.meals.size,
            waterMl = snapshot.waterMl,
            proteinGrams = snapshot.proteinGrams,
            exerciseSignature = DailyAiCachePolicy.exerciseSignature(snapshot),
            nutritionPreferenceSignature = DailyAiCachePolicy.nutritionPreferenceSignature(snapshot)
        )
    }

    private fun analysis(): AiDailyAnalysisResult {
        return AiDailyAnalysisResult(
            status = AiDailyStatusLevel.GREEN,
            mealComments = emptyList(),
            waterComment = "Water is balanced.",
            exerciseComment = "Good walk.",
            generalComment = "Good day.",
            suggestion = "Keep going."
        )
    }

    private class FakeDailyAiCacheStore(
        var cachedMessage: CachedDailyAiMessage? = null,
        var cooldownUntilValue: Long = 0L
    ) : DailyAiCacheStore {
        var wasCleared: Boolean = false
        var autoAttempted: Boolean = false
        var savedSnapshot: DailyAiSnapshot? = null
        var savedMessage: String? = null
        var cooldownDuration: Long? = null

        override fun get(
            dateIso: String,
            triggerType: AiDailyTriggerType
        ): CachedDailyAiMessage? = cachedMessage

        override fun save(snapshot: DailyAiSnapshot, aiMessage: String) {
            savedSnapshot = snapshot
            savedMessage = aiMessage
        }

        override fun clear(dateIso: String, triggerType: AiDailyTriggerType) {
            wasCleared = true
            cachedMessage = null
        }

        override fun hasAutoAttempted(
            dateIso: String,
            triggerType: AiDailyTriggerType
        ): Boolean = autoAttempted

        override fun markAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType) {
            autoAttempted = true
        }

        override fun getCooldownUntil(): Long = cooldownUntilValue

        override fun setCooldownFor(milliseconds: Long) {
            cooldownDuration = milliseconds
        }
    }
}
