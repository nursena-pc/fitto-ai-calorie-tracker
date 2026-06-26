package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivePlanPolicyTest {

    @Test
    fun isEmptyPlan_onlyMatchesEmptyPlanId() {
        assertTrue(DailyActivePlanPolicy.isEmptyPlan("none"))
        assertFalse(DailyActivePlanPolicy.isEmptyPlan("balanced"))
    }

    @Test
    fun isCustomPlan_matchesCustomPlanPrefix() {
        assertTrue(
            DailyActivePlanPolicy.isCustomPlan(
                "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}123"
            )
        )
        assertFalse(DailyActivePlanPolicy.isCustomPlan("balanced"))
    }

    @Test
    fun isFastingPlan_preservesExistingNameChecks() {
        assertTrue(
            DailyActivePlanPolicy.isFastingPlan(
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING
            )
        )
        assertTrue(DailyActivePlanPolicy.isFastingPlan("custom_fasting_plan"))
        assertTrue(DailyActivePlanPolicy.isFastingPlan("if_plan"))
        assertFalse(DailyActivePlanPolicy.isFastingPlan("balanced"))
    }

    @Test
    fun shouldUseBasicPlanRenderer_onlyForEmptyAndIntermittentFastingPlan() {
        assertTrue(DailyActivePlanPolicy.shouldUseBasicPlanRenderer("none"))
        assertTrue(
            DailyActivePlanPolicy.shouldUseBasicPlanRenderer(
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING
            )
        )
        assertFalse(
            DailyActivePlanPolicy.shouldUseBasicPlanRenderer("custom_fasting_plan")
        )
        assertFalse(DailyActivePlanPolicy.shouldUseBasicPlanRenderer("balanced"))
    }
}
