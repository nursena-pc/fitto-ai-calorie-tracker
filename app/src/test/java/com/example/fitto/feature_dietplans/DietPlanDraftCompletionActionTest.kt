package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanDraftCompletionActionTest {

    @Test
    fun complete_returnsNotCompletedWhenPlanIsMissing() {
        val store = FakeCustomPlanStore()

        val result = action(store).complete("missing_plan")

        assertSame(DietPlanDraftCompletionResult.NotCompleted, result)
        assertTrue(store.savedPlans.isEmpty())
    }

    @Test
    fun complete_returnsNotCompletedWhenPlanIsNotDraft() {
        val store = FakeCustomPlanStore(existingPlan = plan(isDraft = false))

        val result = action(store).complete(PLAN_ID)

        assertSame(DietPlanDraftCompletionResult.NotCompleted, result)
        assertTrue(store.savedPlans.isEmpty())
    }

    @Test
    fun complete_savesCompletedDraftWithUpdatedTimestamp() {
        val draft = plan(
            isDraft = true,
            updatedAt = OLD_TIME
        )
        val store = FakeCustomPlanStore(existingPlan = draft)

        val result = action(store).complete(PLAN_ID) as DietPlanDraftCompletionResult.Completed

        assertFalse(result.plan.isDraft)
        assertEquals(NOW, result.plan.updatedAt)
        assertEquals(OLD_TIME, result.plan.createdAt)
        assertEquals(draft.title, result.plan.title)
        assertEquals(result.plan, store.savedPlans.single())
    }

    @Test
    fun complete_usesRequestedPlanIdForLookup() {
        val store = FakeCustomPlanStore(existingPlan = plan(isDraft = true))

        action(store).complete(PLAN_ID)

        assertEquals(listOf(PLAN_ID), store.requestedPlanIds)
    }

    private fun action(store: FakeCustomPlanStore): DietPlanDraftCompletionAction {
        return DietPlanDraftCompletionAction(
            customPlanStore = store,
            nowProvider = { NOW }
        )
    }

    private fun plan(
        isDraft: Boolean,
        updatedAt: Long = OLD_TIME
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = PLAN_ID,
            title = "My draft plan",
            description = "Description",
            goalType = CreatePlanGoal.BALANCED.name,
            dailyCalories = 1800,
            mealCount = 3,
            durationDays = 14,
            isPublic = false,
            preferences = listOf("high_protein"),
            exclusions = listOf("milk"),
            mealOrder = listOf("breakfast", "dinner"),
            isDraft = isDraft,
            createdAt = OLD_TIME,
            updatedAt = updatedAt
        )
    }

    private class FakeCustomPlanStore(
        private val existingPlan: CustomDietPlanLocalModel? = null
    ) : DietPlanDraftCompletionAction.CustomPlanStore {
        val requestedPlanIds = mutableListOf<String>()
        val savedPlans = mutableListOf<CustomDietPlanLocalModel>()

        override fun getPlan(planId: String): CustomDietPlanLocalModel? {
            requestedPlanIds += planId
            return existingPlan?.takeIf { plan -> plan.id == planId }
        }

        override fun savePlan(plan: CustomDietPlanLocalModel) {
            savedPlans += plan
        }
    }

    private companion object {
        const val PLAN_ID = "custom_plan_1"
        const val OLD_TIME = 1_000L
        const val NOW = 2_000L
    }
}
