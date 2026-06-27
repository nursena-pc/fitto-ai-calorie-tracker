package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanDetailEffectResolverTest {

    @Test
    fun `saveButtonState returns save state for unsaved plan`() {
        val state = DietPlanDetailEffectResolver.saveButtonState(isSaved = false)

        assertEquals(R.string.diet_plan_detail_save_plan, state.labelRes)
        assertEquals(1f, state.alpha)
    }

    @Test
    fun `saveButtonState returns saved state for favorite plan`() {
        val state = DietPlanDetailEffectResolver.saveButtonState(isSaved = true)

        assertEquals(R.string.diet_plan_detail_saved_plan, state.labelRes)
        assertEquals(0.86f, state.alpha)
    }

    @Test
    fun `favoriteToggled returns added message and refreshes save state`() {
        val effect = DietPlanDetailEffectResolver.favoriteToggled(
            planTitle = "Mediterranean",
            result = DietPlanFavoriteResult(
                planId = "mediterranean",
                isFavorite = true,
                activePlanId = "dash"
            )
        )

        assertEquals(R.string.diet_plans_favorite_added_message, effect.toastMessageRes)
        assertEquals("Mediterranean", effect.toastPlanTitle)
        assertEquals("dash", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldRenderSaveState)
        assertFalse(effect.shouldCloseScreen)
    }

    @Test
    fun `favoriteToggled returns removed message when favorite is removed`() {
        val effect = DietPlanDetailEffectResolver.favoriteToggled(
            planTitle = "Mediterranean",
            result = DietPlanFavoriteResult(
                planId = "mediterranean",
                isFavorite = false,
                activePlanId = null
            )
        )

        assertEquals(R.string.diet_plans_favorite_removed_message, effect.toastMessageRes)
        assertEquals("Mediterranean", effect.toastPlanTitle)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldRenderSaveState)
    }

    @Test
    fun `planApplied returns success message and closes screen`() {
        val effect = DietPlanDetailEffectResolver.planApplied(
            planTitle = "DASH",
            result = DietPlanApplyResult(activePlanId = "dash")
        )

        assertEquals(R.string.diet_plans_apply_success_message, effect.toastMessageRes)
        assertEquals("DASH", effect.toastPlanTitle)
        assertEquals("dash", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldRenderSaveState)
        assertTrue(effect.shouldCloseScreen)
    }
}
