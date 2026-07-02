package com.example.fitto.feature_daily

import androidx.annotation.StringRes

class DailyFastingAction(
    private val isFastingPlan: (String) -> Boolean,
    private val getFastingTypeName: () -> String?,
    private val getCustomFastingHours: () -> Int?,
    private val getRemainingMillis: (String) -> Long,
    private val startFasting: (planId: String, fastingHours: Int) -> Unit,
    private val clearFasting: () -> Unit
) {

    sealed interface LoadResult {
        data object Hidden : LoadResult
        data class Visible(val state: DailyFastingCardState) : LoadResult
    }

    sealed interface StartResult {
        data object Unavailable : StartResult
        data class Started(
            val state: DailyFastingCardState,
            val remainingMillis: Long
        ) : StartResult
    }

    fun load(planId: String): LoadResult {
        if (!isFastingPlan(planId)) {
            return LoadResult.Hidden
        }

        val fastingHours = resolveFastingHours()
        val remainingMillis = getRemainingMillis(planId).coerceAtLeast(0L)
        val countdownState = if (remainingMillis > 0L) {
            DailyFastingUiStateResolver.resolveRunningCountdownState(remainingMillis)
        } else {
            DailyFastingUiStateResolver.resolveIdleCountdownState()
        }

        return LoadResult.Visible(
            buildState(
                planId = planId,
                fastingHours = fastingHours,
                remainingMillis = remainingMillis,
                countdownState = countdownState
            )
        )
    }

    fun start(planId: String): StartResult {
        if (!isFastingPlan(planId)) {
            return StartResult.Unavailable
        }

        val fastingHours = resolveFastingHours()
        startFasting(planId, fastingHours)
        val remainingMillis = getRemainingMillis(planId).coerceAtLeast(0L)

        val countdownState = if (remainingMillis > 0L) {
            DailyFastingUiStateResolver.resolveRunningCountdownState(remainingMillis)
        } else {
            DailyFastingUiStateResolver.resolveCompletedCountdownState()
        }

        return StartResult.Started(
            state = buildState(
                planId = planId,
                fastingHours = fastingHours,
                remainingMillis = remainingMillis,
                countdownState = countdownState
            ),
            remainingMillis = remainingMillis
        )
    }

    fun tick(
        planId: String,
        remainingMillis: Long
    ): DailyFastingCardState {
        val safeRemainingMillis = remainingMillis.coerceAtLeast(0L)
        val fastingHours = resolveFastingHours()

        return buildState(
            planId = planId,
            fastingHours = fastingHours,
            remainingMillis = safeRemainingMillis,
            countdownState = DailyFastingUiStateResolver
                .resolveRunningCountdownState(safeRemainingMillis)
        )
    }

    fun finish(planId: String): DailyFastingCardState {
        clearFasting()
        val fastingHours = resolveFastingHours()

        return buildState(
            planId = planId,
            fastingHours = fastingHours,
            remainingMillis = 0L,
            countdownState = DailyFastingUiStateResolver.resolveIdleCountdownState()
        )
    }

    fun complete(planId: String): DailyFastingCardState {
        clearFasting()
        val fastingHours = resolveFastingHours()

        return buildState(
            planId = planId,
            fastingHours = fastingHours,
            remainingMillis = 0L,
            countdownState = DailyFastingUiStateResolver.resolveCompletedCountdownState()
        )
    }

    private fun resolveFastingHours(): Int {
        return DailyFastingPolicy.resolveFastingHours(
            fastingTypeName = getFastingTypeName(),
            customFastingHours = getCustomFastingHours()
        )
    }

    private fun buildState(
        planId: String,
        fastingHours: Int,
        remainingMillis: Long,
        countdownState: DailyFastingUiStateResolver.CountdownState
    ): DailyFastingCardState {
        return DailyFastingCardState(
            planId = planId,
            fastingHours = fastingHours,
            mealSuggestionRes = DailyFastingTextResolver
                .resolveMealSuggestionRes(fastingHours),
            countdownState = countdownState,
            analysisState = DailyFastingUiStateResolver.resolveAnalysisState(
                fastingHours = fastingHours,
                remainingMillis = remainingMillis
            )
        )
    }
}

data class DailyFastingCardState(
    val planId: String,
    val fastingHours: Int,
    @StringRes val mealSuggestionRes: Int,
    val countdownState: DailyFastingUiStateResolver.CountdownState,
    val analysisState: DailyFastingUiStateResolver.AnalysisState
)
