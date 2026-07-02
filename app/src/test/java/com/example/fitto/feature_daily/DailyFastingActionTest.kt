package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyFastingActionTest {

    @Test
    fun `non fasting plan returns hidden without reading session`() {
        var remainingRead = false
        val action = createAction(
            isFastingPlan = { false },
            getRemainingMillis = {
                remainingRead = true
                10_000L
            }
        )

        assertEquals(
            DailyFastingAction.LoadResult.Hidden,
            action.load("balanced")
        )
        assertFalse(remainingRead)
    }

    @Test
    fun `idle custom plan resolves hours suggestion and not started analysis`() {
        val action = createAction(
            fastingTypeName = "CUSTOM",
            customFastingHours = 20,
            remainingMillis = 0L
        )

        val result = action.load("intermittent_fasting")
            as DailyFastingAction.LoadResult.Visible

        assertEquals("intermittent_fasting", result.state.planId)
        assertEquals(20, result.state.fastingHours)
        assertEquals(R.string.daily_fasting_meal_suggestion_long, result.state.mealSuggestionRes)
        assertEquals(
            DailyFastingUiStateResolver.ButtonAction.START,
            result.state.countdownState.buttonAction
        )
        assertEquals(
            R.string.daily_fasting_analysis_not_started_title,
            result.state.analysisState.rangeTitleRes
        )
    }

    @Test
    fun `running plan returns remaining time and elapsed analysis`() {
        val remainingMillis = 11L * HOUR_MILLIS
        val action = createAction(
            fastingTypeName = "16_8",
            remainingMillis = remainingMillis
        )

        val result = action.load("intermittent_fasting")
            as DailyFastingAction.LoadResult.Visible

        assertEquals(remainingMillis, result.state.countdownState.remainingMillis)
        assertEquals(
            DailyFastingUiStateResolver.ButtonAction.FINISH,
            result.state.countdownState.buttonAction
        )
        assertTrue(result.state.countdownState.shouldAnimateHourglass)
        assertEquals(
            R.string.daily_fasting_elapsed_range_4_8,
            result.state.analysisState.rangeTitleRes
        )
    }

    @Test
    fun `start persists resolved hours and returns running state`() {
        var startedPlanId: String? = null
        var startedHours: Int? = null
        var remainingMillis = 0L
        val action = createAction(
            fastingTypeName = "15_9",
            getRemainingMillis = { remainingMillis },
            startFasting = { planId, fastingHours ->
                startedPlanId = planId
                startedHours = fastingHours
                remainingMillis = fastingHours * HOUR_MILLIS
            }
        )

        val result = action.start("intermittent_fasting")
            as DailyFastingAction.StartResult.Started

        assertEquals("intermittent_fasting", startedPlanId)
        assertEquals(15, startedHours)
        assertEquals(15L * HOUR_MILLIS, result.remainingMillis)
        assertEquals(
            DailyFastingUiStateResolver.ButtonAction.FINISH,
            result.state.countdownState.buttonAction
        )
    }

    @Test
    fun `start on unavailable plan does not persist a session`() {
        var startCalled = false
        val action = createAction(
            isFastingPlan = { false },
            startFasting = { _, _ -> startCalled = true }
        )

        assertEquals(
            DailyFastingAction.StartResult.Unavailable,
            action.start("balanced")
        )
        assertFalse(startCalled)
    }

    @Test
    fun `zero remaining time after start resolves completed state`() {
        val action = createAction(
            remainingMillis = 0L
        )

        val result = action.start("intermittent_fasting")
            as DailyFastingAction.StartResult.Started

        assertEquals(0L, result.remainingMillis)
        assertEquals(
            DailyFastingUiStateResolver.ButtonAction.NONE,
            result.state.countdownState.buttonAction
        )
        assertEquals(
            R.string.daily_fasting_completed,
            result.state.countdownState.messageRes
        )
    }

    @Test
    fun `tick clamps remaining time and rebuilds analysis`() {
        val action = createAction(fastingTypeName = "12_12")

        val state = action.tick(
            planId = "intermittent_fasting",
            remainingMillis = -50L
        )

        assertEquals(0L, state.countdownState.remainingMillis)
        assertEquals(
            R.string.daily_fasting_analysis_not_started_title,
            state.analysisState.rangeTitleRes
        )
    }

    @Test
    fun `manual finish clears session and returns idle state`() {
        var clearCount = 0
        val action = createAction(
            clearFasting = { clearCount += 1 }
        )

        val state = action.finish("intermittent_fasting")

        assertEquals(1, clearCount)
        assertEquals(
            DailyFastingUiStateResolver.ButtonAction.START,
            state.countdownState.buttonAction
        )
        assertEquals(
            R.string.daily_fasting_not_started,
            state.countdownState.messageRes
        )
    }

    @Test
    fun `timer completion clears session and returns completed state`() {
        var clearCount = 0
        val action = createAction(
            clearFasting = { clearCount += 1 }
        )

        val state = action.complete("intermittent_fasting")

        assertEquals(1, clearCount)
        assertEquals(
            DailyFastingUiStateResolver.ButtonAction.NONE,
            state.countdownState.buttonAction
        )
        assertEquals(
            R.string.daily_fasting_completed,
            state.countdownState.messageRes
        )
    }

    private fun createAction(
        isFastingPlan: (String) -> Boolean = { true },
        fastingTypeName: String? = "16_8",
        customFastingHours: Int? = null,
        remainingMillis: Long = 8L * HOUR_MILLIS,
        getRemainingMillis: (String) -> Long = { remainingMillis },
        startFasting: (String, Int) -> Unit = { _, _ -> },
        clearFasting: () -> Unit = {}
    ): DailyFastingAction {
        return DailyFastingAction(
            isFastingPlan = isFastingPlan,
            getFastingTypeName = { fastingTypeName },
            getCustomFastingHours = { customFastingHours },
            getRemainingMillis = getRemainingMillis,
            startFasting = startFasting,
            clearFasting = clearFasting
        )
    }

    private companion object {
        const val HOUR_MILLIS = 60L * 60L * 1000L
    }
}
