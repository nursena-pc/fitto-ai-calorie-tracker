package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanCatalogTest {

    @Test
    fun getReadyPlans_containsExpectedReadyPlansInCatalogOrder() {
        val plans = DietPlanCatalog.getReadyPlans()

        assertEquals(
            listOf(
                DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
                DietPlanDetailFragment.PLAN_KETOGENIC,
                DietPlanDetailFragment.PLAN_DASH,
                DietPlanDetailFragment.PLAN_MIND,
                DietPlanDetailFragment.PLAN_ELIMINATION,
                DietPlanDetailFragment.PLAN_PLANT_BASED,
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN
            ),
            plans.map(DietPlanUiModel::id)
        )
    }

    @Test
    fun getReadyPlans_appliesFavoriteAndActiveState() {
        val plans = DietPlanCatalog.getReadyPlans(
            favoriteIds = setOf(DietPlanDetailFragment.PLAN_DASH),
            activePlanId = DietPlanDetailFragment.PLAN_DASH
        )
        val dashPlan = plans.first { plan -> plan.id == DietPlanDetailFragment.PLAN_DASH }
        val mediterraneanPlan = plans.first { plan -> plan.id == DietPlanDetailFragment.PLAN_MEDITERRANEAN }

        assertTrue(dashPlan.isFavorite)
        assertTrue(dashPlan.isApplied)
        assertFalse(mediterraneanPlan.isFavorite)
        assertFalse(mediterraneanPlan.isApplied)
    }

    @Test
    fun getReadyPlans_usesSharedKetogenicDuration() {
        val ketogenicPlan = DietPlanCatalog.getReadyPlans()
            .first { plan -> plan.id == DietPlanDetailFragment.PLAN_KETOGENIC }

        assertEquals(21, ketogenicPlan.durationDays)
    }
}
