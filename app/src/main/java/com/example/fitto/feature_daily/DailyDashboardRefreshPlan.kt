package com.example.fitto.feature_daily

enum class DailyDashboardRefreshStep {
    RENDER_SELECTED_DATE,
    LOAD_HEALTH_CONNECT_SUMMARY,
    RENDER_ACTIVE_PLAN,
    START_LOCAL_STEP_COUNTER,
    RENDER_WATER_TRACKING,
    LOAD_PROFILE_METRICS,
    RENDER_SLEEP_CARD,
    RENDER_SLEEP_WEIGHT_SUMMARY,
    RENDER_WEEKLY_CALORIE_PROGRESS,
    RENDER_MEAL_TOTALS
}

data class DailyDashboardRefreshPlan private constructor(
    val steps: List<DailyDashboardRefreshStep>
) {
    fun includes(step: DailyDashboardRefreshStep): Boolean = step in steps

    companion object {
        fun of(vararg steps: DailyDashboardRefreshStep): DailyDashboardRefreshPlan {
            return DailyDashboardRefreshPlan(steps.distinct())
        }
    }
}

object DailyDashboardRefreshPlans {
    val INITIAL = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.RENDER_SELECTED_DATE,
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
        DailyDashboardRefreshStep.RENDER_WATER_TRACKING,
        DailyDashboardRefreshStep.RENDER_SLEEP_CARD,
        DailyDashboardRefreshStep.RENDER_SLEEP_WEIGHT_SUMMARY,
        DailyDashboardRefreshStep.START_LOCAL_STEP_COUNTER,
        DailyDashboardRefreshStep.LOAD_PROFILE_METRICS,
        DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY,
        DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
    )

    val RESUME = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.RENDER_SELECTED_DATE,
        DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY,
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
        DailyDashboardRefreshStep.START_LOCAL_STEP_COUNTER,
        DailyDashboardRefreshStep.RENDER_WATER_TRACKING,
        DailyDashboardRefreshStep.LOAD_PROFILE_METRICS,
        DailyDashboardRefreshStep.RENDER_SLEEP_CARD,
        DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
    )

    val SELECTED_DATE_CHANGED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY,
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
        DailyDashboardRefreshStep.START_LOCAL_STEP_COUNTER,
        DailyDashboardRefreshStep.RENDER_WATER_TRACKING,
        DailyDashboardRefreshStep.RENDER_SLEEP_CARD,
        DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
    )

    val HEALTH_CONNECT_CHANGED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY
    )

    val DIET_PLAN_CHANGED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN
    )

    val MEAL_CHANGED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
        DailyDashboardRefreshStep.RENDER_WEEKLY_CALORIE_PROGRESS
    )

    val EXERCISE_CHANGED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.LOAD_HEALTH_CONNECT_SUMMARY,
        DailyDashboardRefreshStep.RENDER_MEAL_TOTALS,
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN
    )

    val ACTIVE_PLAN_APPLIED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN,
        DailyDashboardRefreshStep.RENDER_MEAL_TOTALS
    )

    val ACTIVE_PLAN_PROGRESS_CHANGED = DailyDashboardRefreshPlan.of(
        DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN
    )
}
