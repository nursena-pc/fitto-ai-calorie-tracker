package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DietPlanDestinationResolverTest {

    @Test
    fun resolve_returnsFastingDestinationForIntermittentFastingPlan() {
        assertSame(
            DietPlanDestination.IntermittentFasting,
            DietPlanDestinationResolver.resolve(DietPlanCatalog.PLAN_INTERMITTENT_FASTING)
        )
    }

    @Test
    fun resolve_returnsReadyDetailForKnownReadyPlan() {
        assertEquals(
            DietPlanDestination.ReadyDetail(DietPlanDetailFragment.PLAN_MEDITERRANEAN),
            DietPlanDestinationResolver.resolve(DietPlanDetailFragment.PLAN_MEDITERRANEAN)
        )
    }

    @Test
    fun resolve_returnsCustomDetailForCustomPlan() {
        val planId = "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}123"

        assertEquals(
            DietPlanDestination.CustomDetail(planId),
            DietPlanDestinationResolver.resolve(planId)
        )
    }

    @Test
    fun resolve_returnsUnknownForUnsupportedPlan() {
        assertEquals(
            DietPlanDestination.Unknown("unknown"),
            DietPlanDestinationResolver.resolve("unknown")
        )
    }
}
