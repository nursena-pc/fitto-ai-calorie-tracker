package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyDashboardRefreshCoordinatorTest {

    @Test
    fun `refresh executes steps once in declared order`() {
        val calls = mutableListOf<DailyDashboardRefreshStep>()
        val coordinator = coordinatorRecording(calls)
        val plan = DailyDashboardRefreshPlan.of(
            DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
            DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
            DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
            DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
        )

        coordinator.refresh(plan)

        assertEquals(
            listOf(
                DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
                DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
                DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
            ),
            calls
        )
    }

    @Test
    fun `initial plan preserves existing initial dashboard order`() {
        assertEquals(
            listOf(
                DailyDashboardRefreshStep.RENDER_SELECTED_DATE,
                DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
                DailyDashboardRefreshStep.RENDER_WATER_TRACKING,
                DailyDashboardRefreshStep.RENDER_SLEEP_CARD,
                DailyDashboardRefreshStep.RENDER_SLEEP_WEIGHT_SUMMARY,
                DailyDashboardRefreshStep.START_LOCAL_STEP_COUNTER,
                DailyDashboardRefreshStep.LOAD_PROFILE_METRICS,
                DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY,
                DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
            ),
            DailyDashboardRefreshPlans.INITIAL.steps
        )
    }

    @Test
    fun `resume plan includes profile metrics but date change plan does not`() {
        assertTrue(
            DailyDashboardRefreshPlans.RESUME.includes(
                DailyDashboardRefreshStep.LOAD_PROFILE_METRICS
            )
        )
        assertFalse(
            DailyDashboardRefreshPlans.SELECTED_DATE_CHANGED.includes(
                DailyDashboardRefreshStep.LOAD_PROFILE_METRICS
            )
        )
    }

    @Test
    fun `meal changed plan refreshes totals plan and weekly progress`() {
        val plan = DailyDashboardRefreshPlans.MEAL_CHANGED

        assertEquals(
            listOf(
                DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
                DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
                DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
            ),
            plan.steps
        )
    }

    private fun coordinatorRecording(
        calls: MutableList<DailyDashboardRefreshStep>
    ): DailyDashboardRefreshCoordinator {
        return DailyDashboardRefreshCoordinator(
            renderSelectedDate = {
                calls += DailyDashboardRefreshStep.RENDER_SELECTED_DATE
            },
            loadHealthConnectSummary = {
                calls += DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY
            },
            renderActivePlan = {
                calls += DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN
            },
            startLocalStepCounter = {
                calls += DailyDashboardRefreshStep.START_LOCAL_STEP_COUNTER
            },
            renderWaterTracking = {
                calls += DailyDashboardRefreshStep.RENDER_WATER_TRACKING
            },
            loadProfileMetrics = {
                calls += DailyDashboardRefreshStep.LOAD_PROFILE_METRICS
            },
            renderSleepCard = {
                calls += DailyDashboardRefreshStep.RENDER_SLEEP_CARD
            },
            renderSleepWeightSummary = {
                calls += DailyDashboardRefreshStep.RENDER_SLEEP_WEIGHT_SUMMARY
            },
            renderWeeklyCalorieProgress = {
                calls += DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
            },
            renderMealTotals = {
                calls += DailyDashboardRefreshStep.RENDER_MEAL_TOTALS
            }
        )
    }
}
