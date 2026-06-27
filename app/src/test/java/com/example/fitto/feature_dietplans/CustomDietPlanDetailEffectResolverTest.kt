package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDietPlanDetailEffectResolverTest {

    @Test
    fun `favorite toggled to saved returns added toast and save render effect`() {
        val effect = CustomDietPlanDetailEffectResolver.favoriteToggled(
            planTitle = "My Plan",
            result = DietPlanFavoriteResult(
                planId = "custom_1",
                isFavorite = true,
                activePlanId = "active_plan"
            )
        )

        assertEquals(R.string.diet_plans_favorite_added_message, effect.toastMessageRes)
        assertEquals("My Plan", effect.toastPlanTitle)
        assertEquals("active_plan", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldRenderSaveState)
        assertFalse(effect.shouldRenderApplyState)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `favorite toggled to unsaved returns removed toast`() {
        val effect = CustomDietPlanDetailEffectResolver.favoriteToggled(
            planTitle = "My Plan",
            result = DietPlanFavoriteResult(
                planId = "custom_1",
                isFavorite = false,
                activePlanId = null
            )
        )

        assertEquals(R.string.diet_plans_favorite_removed_message, effect.toastMessageRes)
        assertEquals("My Plan", effect.toastPlanTitle)
        assertNull(effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldRenderSaveState)
    }

    @Test
    fun `plan applied returns apply toast and apply render effect`() {
        val effect = CustomDietPlanDetailEffectResolver.planApplied(
            planTitle = "My Plan",
            result = DietPlanApplyResult(activePlanId = "custom_1")
        )

        assertEquals(R.string.diet_plans_apply_success, effect.toastMessageRes)
        assertEquals("My Plan", effect.toastPlanTitle)
        assertEquals("custom_1", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldRenderSaveState)
        assertTrue(effect.shouldRenderApplyState)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `plan deleted returns success toast notify and close effect`() {
        val effect = CustomDietPlanDetailEffectResolver.planDeleted(
            CustomDietPlanDeletionResult.Deleted(activePlanId = "balanced")
        )

        assertEquals(R.string.diet_plans_delete_custom_plan_success, effect.toastMessageRes)
        assertEquals("balanced", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldRenderSaveState)
        assertFalse(effect.shouldRenderApplyState)
        assertTrue(effect.shouldCloseScreen)
    }

    @Test
    fun `plan delete failure returns error toast only`() {
        val effect = CustomDietPlanDetailEffectResolver.planDeleted(
            CustomDietPlanDeletionResult.Failed(IllegalStateException("boom"))
        )

        assertEquals(R.string.diet_plans_delete_custom_plan_error, effect.toastMessageRes)
        assertNull(effect.toastPlanTitle)
        assertNull(effect.activePlanId)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `not custom plan delete returns empty effect`() {
        val effect = CustomDietPlanDetailEffectResolver.planDeleted(
            CustomDietPlanDeletionResult.NotCustomPlan
        )

        assertNull(effect.toastMessageRes)
        assertNull(effect.activePlanId)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldRenderSaveState)
        assertFalse(effect.shouldRenderApplyState)
        assertFalse(effect.shouldCloseScreen)
    }
}
