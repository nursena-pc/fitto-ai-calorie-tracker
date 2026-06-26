package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DietPlanDetailCatalogTest {

    @Test
    fun all_containsEveryReadyDietDetailPlan() {
        assertEquals(
            setOf(
                DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                DietPlanDetailFragment.PLAN_KETOGENIC,
                DietPlanDetailFragment.PLAN_DASH,
                DietPlanDetailFragment.PLAN_MIND,
                DietPlanDetailFragment.PLAN_ELIMINATION,
                DietPlanDetailFragment.PLAN_PLANT_BASED,
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN
            ),
            DietPlanDetailCatalog.all.keys
        )
    }

    @Test
    fun get_returnsExpectedDetailResourcesForKnownPlans() {
        val mediterranean = DietPlanDetailCatalog.get(
            DietPlanDetailFragment.PLAN_MEDITERRANEAN
        )
        val ketogenic = DietPlanDetailCatalog.get(
            DietPlanDetailFragment.PLAN_KETOGENIC
        )

        assertEquals(R.string.diet_plan_detail_title_mediterranean, mediterranean?.titleRes)
        assertEquals(4, mediterranean?.featuredFoodCount)
        assertEquals(R.string.keto_detail_title, ketogenic?.titleRes)
        assertEquals(6, ketogenic?.featuredFoodCount)
    }

    @Test
    fun get_returnsNullForUnknownPlan() {
        assertNull(DietPlanDetailCatalog.get("unknown"))
    }
}
