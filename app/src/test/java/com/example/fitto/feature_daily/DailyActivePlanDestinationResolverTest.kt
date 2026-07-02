package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivePlanDestinationResolverTest {

    @Test
    fun `custom plan opens diet plan list`() {
        assertEquals(
            DailyActivePlanDestination.DietPlans,
            DailyActivePlanDestinationResolver.resolve(
                CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX + "weekly"
            )
        )
    }

    @Test
    fun `intermittent fasting opens fasting detail`() {
        assertEquals(
            DailyActivePlanDestination.IntermittentFastingDetail,
            DailyActivePlanDestinationResolver.resolve(
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING
            )
        )
    }

    @Test
    fun `standard plan opens diet plan detail with id`() {
        assertEquals(
            DailyActivePlanDestination.DietPlanDetail("mediterranean"),
            DailyActivePlanDestinationResolver.resolve("mediterranean")
        )
    }
}
