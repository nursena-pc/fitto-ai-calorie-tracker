package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_daily.ai.CachedDailyAiMessage
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAiDashboardCoordinatorTest {

    @Test
    fun `card click without snapshot prepares and refreshes snapshot`() {
        val coordinator = coordinator()

        assertEquals(
            listOf(
                DailyAiDashboardCoordinator.Effect.Preparing,
                DailyAiDashboardCoordinator.Effect.RefreshSnapshot
            ),
            coordinator.onCardClicked()
        )
    }

    @Test
    fun `past day summary renders local message without auto request`() {
        val snapshot = snapshot(dateIso = "2026-07-04")
        val coordinator = coordinator(snapshotBuilder = FakeSnapshotBuilder(snapshot))

        val effects = coordinator.onSummaryLoaded(
            request = summaryRequest(snapshot.dateIso),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )

        assertEquals(
            listOf(
                DailyAiDashboardCoordinator.Effect.Message(
                    message = "local-CALORIE_EXCEEDED",
                    subtitle = DailyAiDashboardCoordinator.Subtitle.DEFAULT,
                    status = AiDailyStatusLevel.RED
                )
            ),
            effects
        )
    }

    @Test
    fun `current exceeded summary starts one automatic request`() {
        val snapshot = snapshot()
        val coordinator = coordinator(snapshotBuilder = FakeSnapshotBuilder(snapshot))

        val effects = coordinator.onSummaryLoaded(
            request = summaryRequest(snapshot.dateIso),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )

        assertTrue(effects[0] is DailyAiDashboardCoordinator.Effect.Message)
        assertEquals(
            DailyAiDashboardCoordinator.Effect.Loading(AiDailyStatusLevel.RED),
            effects[1]
        )
        assertTrue(effects[2] is DailyAiDashboardCoordinator.Effect.Execute)
    }

    @Test
    fun `manual click after summary starts request`() {
        val snapshot = snapshot(dateIso = "2026-07-04")
        val coordinator = coordinator(snapshotBuilder = FakeSnapshotBuilder(snapshot))
        coordinator.onSummaryLoaded(
            request = summaryRequest(snapshot.dateIso),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )

        val effects = coordinator.onCardClicked()

        assertEquals(
            DailyAiDashboardCoordinator.Effect.Loading(AiDailyStatusLevel.RED),
            effects[0]
        )
        assertTrue(effects[1] is DailyAiDashboardCoordinator.Effect.Execute)
    }

    @Test
    fun `active cooldown renders rate limited fallback`() {
        val snapshot = snapshot(dateIso = "2026-07-04")
        val cache = FakeDailyAiCacheStore(cooldownUntil = 3_000L)
        val coordinator = coordinator(
            cache = cache,
            snapshotBuilder = FakeSnapshotBuilder(snapshot),
            nowMillis = { 1_000L }
        )
        coordinator.onSummaryLoaded(
            request = summaryRequest(snapshot.dateIso),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )

        assertEquals(
            listOf(
                DailyAiDashboardCoordinator.Effect.RateLimited(
                    localMessage = "local-CALORIE_EXCEEDED",
                    status = AiDailyStatusLevel.RED
                )
            ),
            coordinator.onCardClicked()
        )
    }

    @Test
    fun `successful request maps to structured result`() = runBlocking {
        val snapshot = snapshot(dateIso = "2026-07-04")
        val analysis = analysis()
        val coordinator = coordinator(
            snapshotBuilder = FakeSnapshotBuilder(snapshot),
            requestAnalysis = { analysis }
        )
        coordinator.onSummaryLoaded(
            request = summaryRequest(snapshot.dateIso),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )
        val request = coordinator.onCardClicked()
            .filterIsInstance<DailyAiDashboardCoordinator.Effect.Execute>()
            .single()
            .request

        val result = coordinator.execute(request)
        val effects = coordinator.onRequestCompleted(
            result = result,
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )

        assertEquals(
            listOf(
                DailyAiDashboardCoordinator.Effect.StructuredResult(
                    analysis = analysis,
                    snapshot = snapshot
                )
            ),
            effects
        )
    }

    @Test
    fun `request failure maps local fallback and failure kind`() = runBlocking {
        val snapshot = snapshot(dateIso = "2026-07-04")
        val coordinator = coordinator(
            snapshotBuilder = FakeSnapshotBuilder(snapshot),
            requestAnalysis = { error("network unavailable") }
        )
        coordinator.onSummaryLoaded(
            request = summaryRequest(snapshot.dateIso),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )
        val request = coordinator.onCardClicked()
            .filterIsInstance<DailyAiDashboardCoordinator.Effect.Execute>()
            .single()
            .request

        val effects = coordinator.onRequestCompleted(
            result = coordinator.execute(request),
            selectedDate = LocalDate.parse(snapshot.dateIso)
        )

        val failure = effects.single() as DailyAiDashboardCoordinator.Effect.Failure
        assertEquals("local-CALORIE_EXCEEDED", failure.localMessage)
        assertEquals(AiDailyStatusLevel.RED, failure.status)
    }

    @Test
    fun `water change updates current snapshot through builder`() {
        val initial = snapshot(dateIso = "2026-07-04")
        val builder = FakeSnapshotBuilder(initial)
        val coordinator = coordinator(snapshotBuilder = builder)
        coordinator.onSummaryLoaded(
            request = summaryRequest(initial.dateIso),
            selectedDate = LocalDate.parse(initial.dateIso)
        )

        coordinator.onWaterChanged(
            waterMl = 1_750,
            waterGoalMl = 2_500,
            selectedDate = LocalDate.parse(initial.dateIso)
        )

        assertEquals(1_750, builder.updatedWaterMl)
        assertEquals(2_500, builder.updatedWaterGoalMl)
    }

    @Test
    fun `stale request resolves the latest snapshot after completion`() = runBlocking {
        val first = snapshot(dateIso = "2026-07-04", userName = "First")
        val second = snapshot(dateIso = "2026-07-03", userName = "Second")
        val builder = DateSnapshotBuilder(mapOf(first.dateIso to first, second.dateIso to second))
        val coordinator = coordinator(snapshotBuilder = builder)
        coordinator.onSummaryLoaded(
            request = summaryRequest(first.dateIso),
            selectedDate = LocalDate.parse(first.dateIso)
        )
        val request = coordinator.onCardClicked()
            .filterIsInstance<DailyAiDashboardCoordinator.Effect.Execute>()
            .single()
            .request

        coordinator.onSummaryLoaded(
            request = summaryRequest(second.dateIso),
            selectedDate = LocalDate.parse(second.dateIso)
        )
        val effects = coordinator.onRequestCompleted(
            result = coordinator.execute(request),
            selectedDate = LocalDate.parse(second.dateIso)
        )

        assertEquals(
            listOf(
                DailyAiDashboardCoordinator.Effect.Message(
                    message = "local-CALORIE_EXCEEDED",
                    subtitle = DailyAiDashboardCoordinator.Subtitle.DEFAULT,
                    status = AiDailyStatusLevel.RED
                )
            ),
            effects
        )
    }

    private fun coordinator(
        cache: FakeDailyAiCacheStore = FakeDailyAiCacheStore(),
        snapshotBuilder: DailyAiSnapshotBuilder = FakeSnapshotBuilder(snapshot()),
        requestAnalysis: suspend () -> AiDailyAnalysisResult = { analysis() },
        nowMillis: () -> Long = { 1_000L }
    ): DailyAiDashboardCoordinator {
        val action = DailyAiAnalysisAction(
            cacheStore = cache,
            requestDailyAnalysis = { _, _ -> requestAnalysis() },
            buildPrompt = { _, _ -> "prompt" },
            currentLanguage = { "tr" },
            buildStructuredMessage = { "structured" },
            resolveLocalMessage = { "local-${it.triggerType.name}" },
            resolveCacheLocalMessages = { emptyList() },
            nowMillis = nowMillis
        )
        return DailyAiDashboardCoordinator(
            analysisAction = action,
            snapshotBuilder = snapshotBuilder,
            profilePreferencesSource = DailyAiProfilePreferencesSource {
                DailyAiProfilePreferences(
                    dietPreferences = "standard",
                    allergies = "",
                    dislikedFoods = "",
                    mealRoutine = "three meals"
                )
            },
            today = { LocalDate.parse("2026-07-05") }
        )
    }

    private fun summaryRequest(dateIso: String): DailyAiSummarySnapshotRequest {
        return DailyAiSummarySnapshotRequest(
            dateIso = dateIso,
            summary = DailyAiTestFixtures.summaryResult(),
            waterMl = 500,
            waterGoalMl = 2_500
        )
    }

    private fun snapshot(
        dateIso: String = "2026-07-05",
        userName: String = "User"
    ): DailyAiSnapshot {
        return DailyAiSnapshot(
            dateIso = dateIso,
            userName = userName,
            goalType = "lose_weight",
            dailyCalorieTarget = 1_800,
            totalCalories = 2_000,
            proteinGrams = 80.0,
            proteinTargetGrams = 120,
            carbGrams = 210.0,
            fatGrams = 70.0,
            waterMl = 500,
            waterGoalMl = 2_500,
            triggerType = AiDailyTriggerType.CALORIE_EXCEEDED,
            meals = emptyList(),
            exerciseLogCount = 0,
            exerciseDurationMinutes = 0,
            exerciseCalories = 0,
            exerciseSummaryText = "",
            dietPreferences = "standard",
            allergies = "",
            dislikedFoods = "",
            mealRoutine = "three meals"
        )
    }

    private fun analysis(): AiDailyAnalysisResult {
        return AiDailyAnalysisResult(
            status = AiDailyStatusLevel.GREEN,
            mealComments = emptyList(),
            waterComment = "water",
            exerciseComment = "exercise",
            generalComment = "general",
            suggestion = "suggestion"
        )
    }

    private class FakeSnapshotBuilder(
        private val snapshot: DailyAiSnapshot
    ) : DailyAiSnapshotBuilder {
        var updatedWaterMl: Int? = null
        var updatedWaterGoalMl: Int? = null

        override fun create(
            request: DailyAiSummarySnapshotRequest,
            preferences: DailyAiProfilePreferences
        ): DailyAiSnapshot = snapshot

        override fun updateWater(
            snapshot: DailyAiSnapshot,
            waterMl: Int,
            waterGoalMl: Int
        ): DailyAiSnapshot {
            updatedWaterMl = waterMl
            updatedWaterGoalMl = waterGoalMl
            return snapshot.copy(waterMl = waterMl, waterGoalMl = waterGoalMl)
        }
    }

    private class DateSnapshotBuilder(
        private val snapshots: Map<String, DailyAiSnapshot>
    ) : DailyAiSnapshotBuilder {
        override fun create(
            request: DailyAiSummarySnapshotRequest,
            preferences: DailyAiProfilePreferences
        ): DailyAiSnapshot = checkNotNull(snapshots[request.dateIso])

        override fun updateWater(
            snapshot: DailyAiSnapshot,
            waterMl: Int,
            waterGoalMl: Int
        ): DailyAiSnapshot = snapshot.copy(waterMl = waterMl, waterGoalMl = waterGoalMl)
    }

    private class FakeDailyAiCacheStore(
        private val cooldownUntil: Long = 0L
    ) : DailyAiCacheStore {
        private val attempted = mutableSetOf<String>()
        private val cached = mutableMapOf<String, CachedDailyAiMessage>()

        override fun get(
            dateIso: String,
            triggerType: AiDailyTriggerType
        ): CachedDailyAiMessage? = cached["$dateIso-${triggerType.name}"]

        override fun save(snapshot: DailyAiSnapshot, aiMessage: String) {
            cached["${snapshot.dateIso}-${snapshot.triggerType.name}"] = CachedDailyAiMessage(
                date = snapshot.dateIso,
                triggerType = snapshot.triggerType,
                aiMessage = aiMessage,
                createdAt = 1_000L,
                source = "test",
                uid = "user",
                totalCalories = snapshot.totalCalories,
                mealCount = snapshot.meals.size,
                waterMl = snapshot.waterMl,
                proteinGrams = snapshot.proteinGrams,
                exerciseSignature = DailyAiCachePolicy.exerciseSignature(snapshot),
                nutritionPreferenceSignature = DailyAiCachePolicy.nutritionPreferenceSignature(snapshot)
            )
        }

        override fun clear(dateIso: String, triggerType: AiDailyTriggerType) {
            cached.remove("$dateIso-${triggerType.name}")
        }

        override fun hasAutoAttempted(
            dateIso: String,
            triggerType: AiDailyTriggerType
        ): Boolean = "$dateIso-${triggerType.name}" in attempted

        override fun markAutoAttempted(dateIso: String, triggerType: AiDailyTriggerType) {
            attempted += "$dateIso-${triggerType.name}"
        }

        override fun getCooldownUntil(): Long = cooldownUntil

        override fun setCooldownFor(milliseconds: Long) = Unit
    }
}
