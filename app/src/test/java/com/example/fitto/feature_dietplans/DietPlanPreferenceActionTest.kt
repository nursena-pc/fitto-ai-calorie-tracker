package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.FastingPlanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanPreferenceActionTest {

    @Test
    fun toggleFavorite_returnsFavoriteStateAndCurrentActivePlan() {
        val store = FakePreferenceStore(
            favoriteResult = true,
            currentActivePlanId = "active_plan"
        )

        val result = DietPlanPreferenceAction(store).toggleFavorite("dash")

        assertEquals("dash", result.planId)
        assertTrue(result.isFavorite)
        assertEquals("active_plan", result.activePlanId)
        assertEquals(listOf("dash"), store.toggledFavoritePlanIds)
    }

    @Test
    fun toggleFavorite_returnsFalseWhenPlanIsRemovedFromFavorites() {
        val store = FakePreferenceStore(favoriteResult = false)

        val result = DietPlanPreferenceAction(store).toggleFavorite("dash")

        assertFalse(result.isFavorite)
        assertNull(result.activePlanId)
    }

    @Test
    fun applyPlan_savesPlanAndMarksItAppliedToday() {
        val store = FakePreferenceStore()

        val result = DietPlanPreferenceAction(store).applyPlan("mediterranean")

        assertEquals("mediterranean", result.activePlanId)
        assertEquals(
            listOf(
                SavedPlan(
                    planId = "mediterranean",
                    fastingType = null,
                    customFastingHours = null
                )
            ),
            store.savedPlans
        )
        assertEquals(1, store.applyTodayCallCount)
    }

    @Test
    fun applyPlan_passesFastingOptions() {
        val store = FakePreferenceStore()

        DietPlanPreferenceAction(store).applyPlan(
            planId = "intermittent_fasting",
            fastingType = FastingPlanType.PLAN_CUSTOM,
            customFastingHours = 18
        )

        assertEquals(
            SavedPlan(
                planId = "intermittent_fasting",
                fastingType = FastingPlanType.PLAN_CUSTOM,
                customFastingHours = 18
            ),
            store.savedPlans.single()
        )
        assertEquals(1, store.applyTodayCallCount)
    }

    private class FakePreferenceStore(
        private val favoriteResult: Boolean = true,
        private val currentActivePlanId: String? = null
    ) : DietPlanPreferenceAction.PreferenceStore {
        val toggledFavoritePlanIds = mutableListOf<String>()
        val savedPlans = mutableListOf<SavedPlan>()
        var applyTodayCallCount = 0

        override fun toggleFavorite(planId: String): Boolean {
            toggledFavoritePlanIds += planId
            return favoriteResult
        }

        override fun saveActivePlan(
            planId: String,
            fastingType: FastingPlanType?,
            customFastingHours: Int?
        ) {
            savedPlans += SavedPlan(
                planId = planId,
                fastingType = fastingType,
                customFastingHours = customFastingHours
            )
        }

        override fun applyActivePlanToday() {
            applyTodayCallCount += 1
        }

        override fun getActivePlanId(): String? {
            return currentActivePlanId
        }
    }

    private data class SavedPlan(
        val planId: String,
        val fastingType: FastingPlanType?,
        val customFastingHours: Int?
    )
}
