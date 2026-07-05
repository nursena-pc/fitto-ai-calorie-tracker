package com.example.fitto.feature_daily

import kotlin.ConsistentCopyVisibility
import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

class DailyAiAnalysisAction(
    private val cacheStore: DailyAiCacheStore,
    private val requestDailyAnalysis: suspend (prompt: String, appLanguage: String) -> AiDailyAnalysisResult,
    private val buildPrompt: (snapshot: DailyAiSnapshot, appLanguage: String) -> String,
    private val currentLanguage: () -> String,
    private val buildStructuredMessage: (AiDailyAnalysisResult) -> String,
    private val resolveLocalMessage: (DailyAiSnapshot) -> String,
    private val resolveCacheLocalMessages: (DailyAiSnapshot) -> List<String>,
    private val resolveFailure: (Throwable) -> DailyAiFailureKind = DailyAiFailureResolver::resolve,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val rateLimitCooldownMillis: Long = DEFAULT_RATE_LIMIT_COOLDOWN_MILLIS
) {

    private var loading: Boolean = false
    private var inFlightRequestKey: String? = null
    private var activeSnapshotKey: String? = null
    private var pendingManualRequest: Boolean = false

    val isLoading: Boolean
        get() = loading

    fun onCardClicked(snapshot: DailyAiSnapshot?): CardClickDecision {
        if (snapshot == null) {
            pendingManualRequest = true
            return CardClickDecision.PrepareSnapshot
        }

        activeSnapshotKey = DailyAiCachePolicy.requestKey(snapshot)
        return CardClickDecision.Request(
            RequestIntent(snapshot = snapshot, isAuto = false)
        )
    }

    fun resolveSnapshot(
        snapshot: DailyAiSnapshot?,
        selectedDate: LocalDate,
        today: LocalDate
    ): SnapshotResolution {
        activeSnapshotKey = snapshot?.let(DailyAiCachePolicy::requestKey)

        if (loading) {
            return SnapshotResolution(SnapshotPresentation.NoChange)
        }

        if (snapshot == null) {
            return SnapshotResolution(SnapshotPresentation.Default)
        }

        if (pendingManualRequest) {
            pendingManualRequest = false
            return SnapshotResolution(
                presentation = SnapshotPresentation.NoChange,
                request = RequestIntent(snapshot = snapshot, isAuto = false)
            )
        }

        usableCachedMessage(snapshot)?.let { cachedMessage ->
            return SnapshotResolution(
                SnapshotPresentation.Message(
                    message = cachedMessage,
                    subtitle = MessageSubtitle.RESULT,
                    status = localStatus(snapshot)
                )
            )
        }

        val localPresentation = SnapshotPresentation.Message(
            message = resolveLocalMessage(snapshot),
            subtitle = MessageSubtitle.DEFAULT,
            status = localStatus(snapshot)
        )
        val shouldAutoRequest = DailyAiPromptDataMapper.shouldAutoRequestAi(
            snapshot = snapshot,
            selectedDate = selectedDate,
            today = today
        ) && !cacheStore.hasAutoAttempted(snapshot.dateIso, snapshot.triggerType)

        return SnapshotResolution(
            presentation = localPresentation,
            request = if (shouldAutoRequest) {
                RequestIntent(snapshot = snapshot, isAuto = true)
            } else {
                null
            }
        )
    }

    fun prepareRequest(intent: RequestIntent): RequestPreparation {
        val snapshot = intent.snapshot
        activeSnapshotKey = activeSnapshotKey ?: DailyAiCachePolicy.requestKey(snapshot)

        usableCachedMessage(snapshot)?.let { cachedMessage ->
            return RequestPreparation.Cached(
                message = cachedMessage,
                status = localStatus(snapshot)
            )
        }

        val requestKey = DailyAiCachePolicy.requestKey(snapshot)
        return when (
            val decision = DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = cacheStore.getCooldownUntil(),
                nowMillis = nowMillis(),
                isLoading = loading,
                inFlightRequestKey = inFlightRequestKey,
                requestKey = requestKey
            )
        ) {
            is DailyAiRequestPolicy.Decision.BlockedByCooldown -> {
                RequestPreparation.BlockedByCooldown(
                    localMessage = resolveLocalMessage(snapshot),
                    status = localStatus(snapshot),
                    remainingSeconds = decision.remainingSeconds
                )
            }

            DailyAiRequestPolicy.Decision.AlreadyLoading,
            DailyAiRequestPolicy.Decision.SameRequestInFlight -> {
                RequestPreparation.AlreadyLoading(localStatus(snapshot))
            }

            DailyAiRequestPolicy.Decision.Start -> {
                if (intent.isAuto) {
                    cacheStore.markAutoAttempted(snapshot.dateIso, snapshot.triggerType)
                }

                loading = true
                inFlightRequestKey = requestKey
                RequestPreparation.Start(
                    PreparedRequest(
                        snapshot = snapshot,
                        requestKey = requestKey
                    )
                )
            }
        }
    }

    suspend fun execute(request: PreparedRequest): RequestResult {
        return try {
            val appLanguage = currentLanguage()
            val prompt = buildPrompt(request.snapshot, appLanguage)
            val analysis = requestDailyAnalysis(prompt, appLanguage)
            val message = buildStructuredMessage(analysis)
                .ifBlank { resolveLocalMessage(request.snapshot) }

            cacheStore.save(request.snapshot, message)

            if (activeSnapshotKey != request.requestKey) {
                RequestResult.Stale
            } else {
                RequestResult.Success(
                    analysis = analysis,
                    snapshot = request.snapshot
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable

            val failureKind = resolveFailure(throwable)
            if (failureKind == DailyAiFailureKind.RATE_LIMIT) {
                cacheStore.setCooldownFor(rateLimitCooldownMillis)
            }

            if (activeSnapshotKey != request.requestKey) {
                RequestResult.Stale
            } else {
                RequestResult.Failure(
                    snapshot = request.snapshot,
                    localMessage = resolveLocalMessage(request.snapshot),
                    failureKind = failureKind
                )
            }
        } finally {
            if (inFlightRequestKey == request.requestKey) {
                loading = false
                inFlightRequestKey = null
            }
        }
    }

    private fun usableCachedMessage(snapshot: DailyAiSnapshot): String? {
        val cachedMessage = cacheStore.get(snapshot.dateIso, snapshot.triggerType)
            ?: return null

        val isUsable = DailyAiCachePolicy.isUsableCachedMessage(
            cachedMessage = cachedMessage,
            snapshot = snapshot,
            localMessages = resolveCacheLocalMessages(snapshot)
        )
        if (!isUsable) {
            cacheStore.clear(snapshot.dateIso, snapshot.triggerType)
            return null
        }

        return cachedMessage.aiMessage.trim()
    }

    private fun localStatus(snapshot: DailyAiSnapshot): AiDailyStatusLevel {
        return DailyAiPromptDataMapper.localStatus(snapshot.triggerType)
    }

    sealed interface CardClickDecision {
        data object PrepareSnapshot : CardClickDecision
        data class Request(val intent: RequestIntent) : CardClickDecision
    }

    data class RequestIntent(
        val snapshot: DailyAiSnapshot,
        val isAuto: Boolean
    )

    data class SnapshotResolution(
        val presentation: SnapshotPresentation,
        val request: RequestIntent? = null
    )

    sealed interface SnapshotPresentation {
        data object NoChange : SnapshotPresentation
        data object Default : SnapshotPresentation
        data class Message(
            val message: String,
            val subtitle: MessageSubtitle,
            val status: AiDailyStatusLevel
        ) : SnapshotPresentation
    }

    enum class MessageSubtitle {
        DEFAULT,
        RESULT
    }

    sealed interface RequestPreparation {
        data class Cached(
            val message: String,
            val status: AiDailyStatusLevel
        ) : RequestPreparation

        data class BlockedByCooldown(
            val localMessage: String,
            val status: AiDailyStatusLevel,
            val remainingSeconds: Long
        ) : RequestPreparation

        data class AlreadyLoading(
            val status: AiDailyStatusLevel
        ) : RequestPreparation

        data class Start(
            val request: PreparedRequest
        ) : RequestPreparation
    }

    @ConsistentCopyVisibility
    data class PreparedRequest internal constructor(
        val snapshot: DailyAiSnapshot,
        val requestKey: String
    )

    sealed interface RequestResult {
        data class Success(
            val analysis: AiDailyAnalysisResult,
            val snapshot: DailyAiSnapshot
        ) : RequestResult

        data class Failure(
            val snapshot: DailyAiSnapshot,
            val localMessage: String,
            val failureKind: DailyAiFailureKind
        ) : RequestResult

        data object Stale : RequestResult
    }

    companion object {
        const val DEFAULT_RATE_LIMIT_COOLDOWN_MILLIS = 60_000L
    }
}
