package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlansScreenStateStoreTest {

    @Test
    fun `selection filter and plans are preserved in one state`() {
        val store = DietPlansScreenStateStore()
        val filter = DietPlanFilterState(
            goal = "balanced",
            duration = 14,
            type = "custom",
            features = setOf("easy"),
            minCalories = 1500,
            maxCalories = 2200
        )
        val plan = plan("custom_1")

        store.selectTab(DietPlanTab.MY)
        store.setFilter(filter)
        store.setPlans(listOf(plan))

        assertEquals(DietPlanTab.MY, store.state.selectedTab)
        assertEquals(filter, store.state.activeFilter)
        assertEquals(listOf(plan), store.state.plans)
        assertNull(store.state.recommendation)
    }

    @Test
    fun `latest recommendation generation accepts its result`() {
        val store = DietPlansScreenStateStore()
        val generation = store.beginRecommendationLoad()
        val recommendation = recommendation("ready_1")

        val applied = store.applyRecommendation(generation, recommendation)

        assertTrue(applied)
        assertEquals(recommendation, store.state.recommendation)
    }

    @Test
    fun `stale recommendation cannot replace latest recommendation`() {
        val store = DietPlansScreenStateStore()
        val staleGeneration = store.beginRecommendationLoad()
        val latestGeneration = store.beginRecommendationLoad()
        val latest = recommendation("latest")

        assertTrue(store.applyRecommendation(latestGeneration, latest))
        assertFalse(store.applyRecommendation(staleGeneration, recommendation("stale")))
        assertEquals(latest, store.state.recommendation)
    }

    private fun plan(id: String) = DietPlanUiModel(
        id = id,
        titleText = id,
        kcalPerDay = 1800,
        durationDays = 14
    )

    private fun recommendation(id: String) = PersonalizedDietRecommendation(
        planId = id,
        titleText = id,
        reasonRes = 1,
        matchPercent = 90,
        chip1Res = 2,
        chip2Res = 3,
        chip3Res = 4
    )
}
