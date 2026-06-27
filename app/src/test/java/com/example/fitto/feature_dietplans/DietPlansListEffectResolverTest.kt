package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlansListEffectResolverTest {

    @Test
    fun `favoriteToggled returns added message and reload effect`() {
        val effect = DietPlansListEffectResolver.favoriteToggled(
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
        assertTrue(effect.shouldRefreshRecommendation)
        assertFalse(effect.shouldNotifyDietPlanChanged)
    }

    @Test
    fun `favoriteToggled returns removed message when plan is no longer favorite`() {
        val effect = DietPlansListEffectResolver.favoriteToggled(
            planTitle = "Protein Plan",
            result = DietPlanFavoriteResult(
                planId = "plan-1",
                isFavorite = false,
                activePlanId = null
            )
        )

        assertEquals(R.string.diet_plans_favorite_removed_message, effect.toastMessageRes)
        assertEquals("Protein Plan", effect.toastPlanTitle)
    }

    @Test
    fun `planApplied notifies active plan and reloads list`() {
        val effect = DietPlansListEffectResolver.planApplied(
            planTitle = "Balanced Plan",
            result = DietPlanApplyResult(activePlanId = "balanced")
        )

        assertEquals(R.string.diet_plans_apply_success, effect.toastMessageRes)
        assertEquals("Balanced Plan", effect.toastPlanTitle)
        assertEquals("balanced", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldRefreshRecommendation)
    }

    @Test
    fun `draftCompleted selects my plans tab and reloads list`() {
        val effect = DietPlansListEffectResolver.draftCompleted()

        assertEquals(R.string.diet_plans_draft_completed, effect.toastMessageRes)
        assertEquals(DietPlansFragment.TAB_MY, effect.selectTab)
        assertEquals(DietPlanTab.MY, effect.selectedTab)
        assertEquals(R.id.chipMyPlans, effect.checkedChipId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldRefreshRecommendation)
    }

    @Test
    fun `customPlanDeleted returns success effect for deleted result`() {
        val effect = DietPlansListEffectResolver.customPlanDeleted(
            CustomDietPlanDeletionResult.Deleted(activePlanId = "ready_low_carb")
        )

        assertEquals(R.string.diet_plans_delete_custom_plan_success, effect.toastMessageRes)
        assertEquals("ready_low_carb", effect.activePlanId)
        assertTrue(effect.shouldNotifyDietPlanChanged)
        assertTrue(effect.shouldReloadPlans)
        assertTrue(effect.shouldRefreshRecommendation)
    }

    @Test
    fun `customPlanDeleted returns error toast for failed result`() {
        val effect = DietPlansListEffectResolver.customPlanDeleted(
            CustomDietPlanDeletionResult.Failed(IllegalStateException("boom"))
        )

        assertEquals(R.string.diet_plans_delete_custom_plan_error, effect.toastMessageRes)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldReloadPlans)
        assertFalse(effect.shouldRefreshRecommendation)
    }

    @Test
    fun `customPlanDeleted returns none for non custom plan`() {
        val effect = DietPlansListEffectResolver.customPlanDeleted(
            CustomDietPlanDeletionResult.NotCustomPlan
        )

        assertNull(effect.toastMessageRes)
        assertFalse(effect.shouldNotifyDietPlanChanged)
        assertFalse(effect.shouldReloadPlans)
        assertFalse(effect.shouldRefreshRecommendation)
    }
}
