package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DietPlanTabResolverTest {

    @Test
    fun fromChipId_mapsKnownChipsToTabs() {
        assertEquals(DietPlanTab.MY, DietPlanTabResolver.fromChipId(R.id.chipMyPlans))
        assertEquals(DietPlanTab.DRAFTS, DietPlanTabResolver.fromChipId(R.id.chipDrafts))
        assertEquals(DietPlanTab.SAVED, DietPlanTabResolver.fromChipId(R.id.chipFavorites))
    }

    @Test
    fun fromChipId_fallsBackToReadyForReadyOrMissingChip() {
        assertEquals(DietPlanTab.READY, DietPlanTabResolver.fromChipId(R.id.chipReadyPlans))
        assertEquals(DietPlanTab.READY, DietPlanTabResolver.fromChipId(null))
        assertEquals(DietPlanTab.READY, DietPlanTabResolver.fromChipId(0))
    }
}
