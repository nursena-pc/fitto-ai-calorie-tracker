package com.example.fitto.feature_dietplans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDietPlanCompletionActionTest {

    private val action = CreateDietPlanCompletionAction()

    @Test
    fun draftSaved_returnsDraftTabAndSavedMessageForNewDraft() {
        val effect = action.draftSaved(wasUpdate = false)

        assertNull(effect.result.activePlanId)
        assertEquals(DietPlansFragment.TAB_DRAFTS, effect.result.selectedTab)
        assertEquals(CreateDietPlanCompletionMessage.DRAFT_SAVED, effect.message)
        assertTrue(effect.closeScreen)
    }

    @Test
    fun draftSaved_returnsDraftTabAndUpdatedMessageForExistingDraft() {
        val effect = action.draftSaved(wasUpdate = true)

        assertNull(effect.result.activePlanId)
        assertEquals(DietPlansFragment.TAB_DRAFTS, effect.result.selectedTab)
        assertEquals(CreateDietPlanCompletionMessage.DRAFT_UPDATED, effect.message)
        assertTrue(effect.closeScreen)
    }

    @Test
    fun draftDeleted_returnsDraftTabAndDeletedMessage() {
        val effect = action.draftDeleted()

        assertNull(effect.result.activePlanId)
        assertEquals(DietPlansFragment.TAB_DRAFTS, effect.result.selectedTab)
        assertEquals(CreateDietPlanCompletionMessage.DRAFT_DELETED, effect.message)
        assertTrue(effect.closeScreen)
    }

    @Test
    fun planCreated_returnsMyPlansTabAndActivePlan() {
        val effect = action.planCreated(activePlanId = "custom_plan_42")

        assertEquals("custom_plan_42", effect.result.activePlanId)
        assertEquals(DietPlansFragment.TAB_MY, effect.result.selectedTab)
        assertEquals(CreateDietPlanCompletionMessage.PLAN_CREATED, effect.message)
        assertTrue(effect.closeScreen)
    }
}
