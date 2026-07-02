package com.example.fitto.feature_daily

class DailyDashboardRefreshCoordinator(
    private val renderSelectedDate: () -> Unit,
    private val loadHealthConnectSummary: () -> Unit,
    private val renderActivePlan: () -> Unit,
    private val startLocalStepCounter: () -> Unit,
    private val renderWaterTracking: () -> Unit,
    private val loadProfileMetrics: () -> Unit,
    private val renderSleepCard: () -> Unit,
    private val renderSleepWeightSummary: () -> Unit,
    private val renderWeeklyCalorieProgress: () -> Unit,
    private val renderMealTotals: () -> Unit
) {
    fun refresh(plan: DailyDashboardRefreshPlan) {
        plan.steps.forEach { step ->
            when (step) {
                DailyDashboardRefreshStep.RENDER_SELECTED_DATE -> renderSelectedDate()
                DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY -> {
                    loadHealthConnectSummary()
                }
                DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN -> renderActivePlan()
                DailyDashboardRefreshStep.START_LOCAL_STEP_COUNTER -> startLocalStepCounter()
                DailyDashboardRefreshStep.RENDER_WATER_TRACKING -> renderWaterTracking()
                DailyDashboardRefreshStep.LOAD_PROFILE_METRICS -> loadProfileMetrics()
                DailyDashboardRefreshStep.RENDER_SLEEP_CARD -> renderSleepCard()
                DailyDashboardRefreshStep.RENDER_SLEEP_WEIGHT_SUMMARY -> {
                    renderSleepWeightSummary()
                }
                DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS -> {
                    renderWeeklyCalorieProgress()
                }
                DailyDashboardRefreshStep.RENDER_MEAL_TOTALS -> renderMealTotals()
            }
        }
    }
}
