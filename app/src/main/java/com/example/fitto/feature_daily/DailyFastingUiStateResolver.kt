package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R

object DailyFastingUiStateResolver {

    enum class ButtonAction {
        START,
        FINISH,
        NONE
    }

    data class CountdownState(
        @StringRes val titleRes: Int,
        @StringRes val messageRes: Int?,
        val remainingMillis: Long?,
        @StringRes val buttonTextRes: Int,
        val isButtonEnabled: Boolean,
        val buttonAction: ButtonAction,
        val shouldAnimateHourglass: Boolean
    )

    data class AnalysisState(
        @StringRes val titleRes: Int,
        @StringRes val rangeTitleRes: Int,
        @StringRes val bodyTextRes: Int
    )

    fun resolveIdleCountdownState(): CountdownState {
        return CountdownState(
            titleRes = R.string.daily_fasting_countdown_title,
            messageRes = R.string.daily_fasting_not_started,
            remainingMillis = null,
            buttonTextRes = R.string.daily_fasting_start_button,
            isButtonEnabled = true,
            buttonAction = ButtonAction.START,
            shouldAnimateHourglass = false
        )
    }

    fun resolveRunningCountdownState(remainingMillis: Long): CountdownState {
        return CountdownState(
            titleRes = R.string.daily_fasting_countdown_title,
            messageRes = null,
            remainingMillis = remainingMillis.coerceAtLeast(0L),
            buttonTextRes = R.string.daily_fasting_finish_button,
            isButtonEnabled = true,
            buttonAction = ButtonAction.FINISH,
            shouldAnimateHourglass = true
        )
    }

    fun resolveCompletedCountdownState(): CountdownState {
        return CountdownState(
            titleRes = R.string.daily_fasting_countdown_title,
            messageRes = R.string.daily_fasting_completed,
            remainingMillis = null,
            buttonTextRes = R.string.daily_fasting_start_button,
            isButtonEnabled = true,
            buttonAction = ButtonAction.NONE,
            shouldAnimateHourglass = false
        )
    }

    fun resolveAnalysisState(
        fastingHours: Int,
        remainingMillis: Long
    ): AnalysisState {
        if (remainingMillis <= 0L) {
            return AnalysisState(
                titleRes = R.string.daily_fasting_analysis_title,
                rangeTitleRes = R.string.daily_fasting_analysis_not_started_title,
                bodyTextRes = R.string.daily_fasting_analysis_not_started_body
            )
        }

        val elapsedHours = DailyFastingPolicy.calculateElapsedHours(
            fastingHours = fastingHours,
            remainingMillis = remainingMillis
        )
        val analysisTextRes = DailyFastingTextResolver.resolveAnalysisTextRes(
            DailyFastingPolicy.resolveAnalysisRange(elapsedHours)
        )

        return AnalysisState(
            titleRes = R.string.daily_fasting_analysis_title,
            rangeTitleRes = analysisTextRes.rangeTitleRes,
            bodyTextRes = analysisTextRes.bodyTextRes
        )
    }
}
