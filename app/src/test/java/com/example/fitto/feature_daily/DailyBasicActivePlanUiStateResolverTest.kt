package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyBasicActivePlanUiStateResolverTest {

    @Test
    fun resolve_returnsFastingContentForFastingPlan() {
        val state = DailyBasicActivePlanUiStateResolver.resolve(
            activePlan(id = DietPlanCatalog.PLAN_INTERMITTENT_FASTING)
        )

        assertEquals(DietPlanCatalog.PLAN_INTERMITTENT_FASTING, state.planId)
        assertEquals("Plan title", state.title)
        assertEquals("Plan badge", state.badge)
        assertEquals(42, state.progress)
        assertEquals(
            DailyBasicActivePlanUiStateResolver.Content.Fasting,
            state.content
        )
    }

    @Test
    fun resolve_returnsMetricContentForRegularPlan() {
        val state = DailyBasicActivePlanUiStateResolver.resolve(
            activePlan(id = "balanced")
        )

        assertEquals(
            DailyBasicActivePlanUiStateResolver.Content.Metrics(
                primaryLabel = "Calories",
                primaryValue = "1800 kcal",
                secondaryLabel = "Duration",
                secondaryValue = "7 days"
            ),
            state.content
        )
    }

    private fun activePlan(id: String): ActiveDietPlanUiModel {
        return ActiveDietPlanUiModel(
            id = id,
            title = "Plan title",
            badge = "Plan badge",
            icon = "icon",
            primaryLabel = "Calories",
            primaryValue = "1800 kcal",
            secondaryLabel = "Duration",
            secondaryValue = "7 days",
            progress = 42,
            kcalPerDay = 1800,
            durationDays = 7
        )
    }
}
