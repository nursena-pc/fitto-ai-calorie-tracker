package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.FastingPlanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FastingPlanContentCatalogTest {

    @Test
    fun get_returnsContentForAllFixedFastingPlans() {
        val fixedPlanTypes = FastingPlanType.values()
            .filterNot { it == FastingPlanType.PLAN_CUSTOM }

        fixedPlanTypes.forEach { planType ->
            val plan = FastingPlanContentCatalog.get(planType)

            assertNotNull(plan)
            assertEquals(planType, plan?.type)
        }
    }

    @Test
    fun get_returnsNullForCustomPlanBecauseItIsRenderedFromUserInput() {
        assertNull(FastingPlanContentCatalog.get(FastingPlanType.PLAN_CUSTOM))
    }
}
