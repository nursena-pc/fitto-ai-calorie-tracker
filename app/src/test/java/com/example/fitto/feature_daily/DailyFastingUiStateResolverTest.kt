package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyFastingUiStateResolverTest {

    @Test
    fun resolveIdleCountdownState_returnsStartReadyState() {
        val state = DailyFastingUiStateResolver.resolveIdleCountdownState()

        assertEquals(R.string.daily_fasting_countdown_title, state.titleRes)
        assertEquals(R.string.daily_fasting_not_started, state.messageRes)
        assertNull(state.remainingMillis)
        assertEquals(R.string.daily_fasting_start_button, state.buttonTextRes)
        assertTrue(state.isButtonEnabled)
        assertEquals(DailyFastingUiStateResolver.ButtonAction.START, state.buttonAction)
        assertFalse(state.shouldAnimateHourglass)
    }

    @Test
    fun resolveRunningCountdownState_returnsFinishStateWithRemainingTime() {
        val state = DailyFastingUiStateResolver.resolveRunningCountdownState(
            remainingMillis = 45_000L
        )

        assertEquals(R.string.daily_fasting_countdown_title, state.titleRes)
        assertNull(state.messageRes)
        assertEquals(45_000L, state.remainingMillis)
        assertEquals(R.string.daily_fasting_finish_button, state.buttonTextRes)
        assertTrue(state.isButtonEnabled)
        assertEquals(DailyFastingUiStateResolver.ButtonAction.FINISH, state.buttonAction)
        assertTrue(state.shouldAnimateHourglass)
    }

    @Test
    fun resolveRunningCountdownState_clampsNegativeRemainingTime() {
        val state = DailyFastingUiStateResolver.resolveRunningCountdownState(
            remainingMillis = -1L
        )

        assertEquals(0L, state.remainingMillis)
    }

    @Test
    fun resolveCompletedCountdownState_returnsCompletedMessageWithoutAction() {
        val state = DailyFastingUiStateResolver.resolveCompletedCountdownState()

        assertEquals(R.string.daily_fasting_completed, state.messageRes)
        assertEquals(R.string.daily_fasting_start_button, state.buttonTextRes)
        assertEquals(DailyFastingUiStateResolver.ButtonAction.NONE, state.buttonAction)
        assertFalse(state.shouldAnimateHourglass)
    }

    @Test
    fun resolveAnalysisState_withoutRemainingTimeReturnsNotStartedTexts() {
        assertEquals(
            DailyFastingUiStateResolver.AnalysisState(
                titleRes = R.string.daily_fasting_analysis_title,
                rangeTitleRes = R.string.daily_fasting_analysis_not_started_title,
                bodyTextRes = R.string.daily_fasting_analysis_not_started_body
            ),
            DailyFastingUiStateResolver.resolveAnalysisState(
                fastingHours = 16,
                remainingMillis = 0L
            )
        )
    }

    @Test
    fun resolveAnalysisState_withRemainingTimeReturnsElapsedRangeTexts() {
        assertEquals(
            DailyFastingUiStateResolver.AnalysisState(
                titleRes = R.string.daily_fasting_analysis_title,
                rangeTitleRes = R.string.daily_fasting_elapsed_range_4_8,
                bodyTextRes = R.string.daily_fasting_elapsed_body_4_8
            ),
            DailyFastingUiStateResolver.resolveAnalysisState(
                fastingHours = 16,
                remainingMillis = 11L * 60L * 60L * 1000L
            )
        )
    }
}
