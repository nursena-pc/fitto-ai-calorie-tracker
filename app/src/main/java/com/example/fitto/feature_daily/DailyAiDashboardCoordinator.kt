package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import java.time.LocalDate

internal class DailyAiDashboardCoordinator(
    private val analysisAction: DailyAiAnalysisAction,
    private val snapshotBuilder: DailyAiSnapshotBuilder,
    private val profilePreferencesSource: DailyAiProfilePreferencesSource,
    private val today: () -> LocalDate = LocalDate::now
) {

    private var currentSnapshot: DailyAiSnapshot? = null

    fun onCardClicked(): List<Effect> {
        return when (val decision = analysisAction.onCardClicked(currentSnapshot)) {
            DailyAiAnalysisAction.CardClickDecision.PrepareSnapshot -> listOf(
                Effect.Preparing,
                Effect.RefreshSnapshot
            )

            is DailyAiAnalysisAction.CardClickDecision.Request -> {
                prepareRequest(decision.intent)
            }
        }
    }

    fun onSummaryLoaded(
        request: DailyAiSummarySnapshotRequest,
        selectedDate: LocalDate
    ): List<Effect> {
        currentSnapshot = snapshotBuilder.create(
            request = request,
            preferences = profilePreferencesSource.load()
        )
        return resolveCurrentSnapshot(selectedDate)
    }

    fun onWaterChanged(
        waterMl: Int,
        waterGoalMl: Int,
        selectedDate: LocalDate
    ): List<Effect> {
        val snapshot = currentSnapshot ?: return emptyList()
        currentSnapshot = snapshotBuilder.updateWater(
            snapshot = snapshot,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl
        )
        return resolveCurrentSnapshot(selectedDate)
    }

    suspend fun execute(
        request: DailyAiAnalysisAction.PreparedRequest
    ): DailyAiAnalysisAction.RequestResult {
        return analysisAction.execute(request)
    }

    fun onRequestCompleted(
        result: DailyAiAnalysisAction.RequestResult,
        selectedDate: LocalDate
    ): List<Effect> {
        return when (result) {
            is DailyAiAnalysisAction.RequestResult.Success -> listOf(
                Effect.StructuredResult(
                    analysis = result.analysis,
                    snapshot = result.snapshot
                )
            )

            is DailyAiAnalysisAction.RequestResult.Failure -> listOf(
                Effect.Failure(
                    localMessage = result.localMessage,
                    failureKind = result.failureKind,
                    status = DailyAiPromptDataMapper.localStatus(result.snapshot.triggerType)
                )
            )

            DailyAiAnalysisAction.RequestResult.Stale -> resolveCurrentSnapshot(selectedDate)
        }
    }

    private fun resolveCurrentSnapshot(selectedDate: LocalDate): List<Effect> {
        val resolution = analysisAction.resolveSnapshot(
            snapshot = currentSnapshot,
            selectedDate = selectedDate,
            today = today()
        )
        return buildList {
            presentationEffect(resolution.presentation)?.let(::add)
            resolution.request?.let { intent -> addAll(prepareRequest(intent)) }
        }
    }

    private fun prepareRequest(
        intent: DailyAiAnalysisAction.RequestIntent
    ): List<Effect> {
        return when (val preparation = analysisAction.prepareRequest(intent)) {
            is DailyAiAnalysisAction.RequestPreparation.Cached -> listOf(
                Effect.Message(
                    message = preparation.message,
                    subtitle = Subtitle.RESULT,
                    status = preparation.status
                )
            )

            is DailyAiAnalysisAction.RequestPreparation.BlockedByCooldown -> listOf(
                Effect.RateLimited(
                    localMessage = preparation.localMessage,
                    status = preparation.status
                )
            )

            is DailyAiAnalysisAction.RequestPreparation.AlreadyLoading -> listOf(
                Effect.Loading(preparation.status)
            )

            is DailyAiAnalysisAction.RequestPreparation.Start -> listOf(
                Effect.Loading(
                    DailyAiPromptDataMapper.localStatus(
                        preparation.request.snapshot.triggerType
                    )
                ),
                Effect.Execute(preparation.request)
            )
        }
    }

    private fun presentationEffect(
        presentation: DailyAiAnalysisAction.SnapshotPresentation
    ): Effect? {
        return when (presentation) {
            DailyAiAnalysisAction.SnapshotPresentation.NoChange -> null
            DailyAiAnalysisAction.SnapshotPresentation.Default -> Effect.Default
            is DailyAiAnalysisAction.SnapshotPresentation.Message -> Effect.Message(
                message = presentation.message,
                subtitle = when (presentation.subtitle) {
                    DailyAiAnalysisAction.MessageSubtitle.DEFAULT -> Subtitle.DEFAULT
                    DailyAiAnalysisAction.MessageSubtitle.RESULT -> Subtitle.RESULT
                },
                status = presentation.status
            )
        }
    }

    internal enum class Subtitle {
        DEFAULT,
        RESULT
    }

    internal sealed interface Effect {
        data object Default : Effect
        data object Preparing : Effect
        data object RefreshSnapshot : Effect

        data class Loading(
            val status: AiDailyStatusLevel
        ) : Effect

        data class Message(
            val message: String,
            val subtitle: Subtitle,
            val status: AiDailyStatusLevel
        ) : Effect

        data class RateLimited(
            val localMessage: String,
            val status: AiDailyStatusLevel
        ) : Effect

        data class Failure(
            val localMessage: String,
            val failureKind: DailyAiFailureKind,
            val status: AiDailyStatusLevel
        ) : Effect

        data class StructuredResult(
            val analysis: AiDailyAnalysisResult,
            val snapshot: DailyAiSnapshot
        ) : Effect

        data class Execute(
            val request: DailyAiAnalysisAction.PreparedRequest
        ) : Effect
    }
}
