package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanSearchEffectResolverTest {

    @Test
    fun `favoriteToggled returns added toast and refreshes visible results`() {
        val effect = DietPlanSearchEffectResolver.favoriteToggled(
            planTitle = "Protein Plan",
            result = DietPlanFavoriteResult(
                planId = "plan-1",
                isFavorite = true,
                activePlanId = null
            )
        )

        assertEquals(R.string.diet_plans_favorite_added_message, effect.toastMessageRes)
        assertEquals("Protein Plan", effect.toastPlanTitle)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldRefilterPlans)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `favoriteToggled returns removed toast when favorite is cleared`() {
        val effect = DietPlanSearchEffectResolver.favoriteToggled(
            planTitle = "Protein Plan",
            result = DietPlanFavoriteResult(
                planId = "plan-1",
                isFavorite = false,
                activePlanId = null
            )
        )

        assertEquals(R.string.diet_plans_favorite_removed_message, effect.toastMessageRes)
        assertEquals("Protein Plan", effect.toastPlanTitle)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldRefilterPlans)
    }

    @Test
    fun `planApplied notifies active plan reloads and closes search screen`() {
        val effect = DietPlanSearchEffectResolver.planApplied(
            planTitle = "Balanced Plan",
            result = DietPlanApplyResult(activePlanId = "balanced")
        )

        assertEquals(R.string.diet_plans_apply_success_message, effect.toastMessageRes)
        assertEquals("Balanced Plan", effect.toastPlanTitle)
        assertEquals("balanced", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldCloseScreen)
        assertFalse(effect.shouldRefilterPlans)
    }

    @Test
    fun `customPlanDeleted returns success effect and refreshes search results`() {
        val effect = DietPlanSearchEffectResolver.customPlanDeleted(
            CustomDietPlanDeletionResult.Deleted(activePlanId = "ready_low_carb")
        )

        assertEquals(R.string.diet_plans_delete_custom_plan_success, effect.toastMessageRes)
        assertEquals("ready_low_carb", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldRefilterPlans)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `customPlanDeleted returns error toast for failed result`() {
        val effect = DietPlanSearchEffectResolver.customPlanDeleted(
            CustomDietPlanDeletionResult.Failed(IllegalStateException("boom"))
        )

        assertEquals(R.string.diet_plans_delete_custom_plan_error, effect.toastMessageRes)
        assertNull(effect.activePlanId)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldReloadPlans)
        assertFalse(effect.shouldRefilterPlans)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `customPlanDeleted returns none for non custom plan`() {
        val effect = DietPlanSearchEffectResolver.customPlanDeleted(
            CustomDietPlanDeletionResult.NotCustomPlan
        )

        assertNull(effect.toastMessageRes)
        assertNull(effect.activePlanId)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldReloadPlans)
        assertFalse(effect.shouldRefilterPlans)
        assertFalse(effect.shouldCloseScreen)
    }
}
