package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDietPlanDeletionActionTest {

    @Test
    fun delete_removesCustomPlanFavoriteAndActivePlan() {
        val planId = customPlanId("active")
        val customPlanStore = FakeCustomPlanStore()
        val preferenceStore = FakePreferenceStore(currentActivePlanId = planId)

        val result = action(customPlanStore, preferenceStore).delete(planId)

        assertTrue(result is CustomDietPlanDeletionResult.Deleted)
        assertNull((result as CustomDietPlanDeletionResult.Deleted).activePlanId)
        assertEquals(listOf(planId), customPlanStore.deletedPlanIds)
        assertEquals(listOf(planId), preferenceStore.removedFavoritePlanIds)
        assertEquals(listOf(planId), preferenceStore.clearedActivePlanIds)
    }

    @Test
    fun delete_preservesDifferentActivePlan() {
        val planId = customPlanId("old")
        val activePlanId = "mediterranean"
        val preferenceStore = FakePreferenceStore(currentActivePlanId = activePlanId)

        val result = action(preferenceStore = preferenceStore).delete(planId)

        assertTrue(result is CustomDietPlanDeletionResult.Deleted)
        assertEquals(activePlanId, (result as CustomDietPlanDeletionResult.Deleted).activePlanId)
        assertEquals(activePlanId, preferenceStore.currentActivePlanId)
    }

    @Test
    fun delete_skipsReadyMadePlans() {
        val customPlanStore = FakeCustomPlanStore()
        val preferenceStore = FakePreferenceStore(currentActivePlanId = "dash")

        val result = action(customPlanStore, preferenceStore).delete("dash")

        assertSame(CustomDietPlanDeletionResult.NotCustomPlan, result)
        assertTrue(customPlanStore.deletedPlanIds.isEmpty())
        assertTrue(preferenceStore.removedFavoritePlanIds.isEmpty())
        assertTrue(preferenceStore.clearedActivePlanIds.isEmpty())
    }

    @Test
    fun delete_returnsFailureWhenStoreThrows() {
        val failure = IllegalStateException("delete failed")
        val planId = customPlanId("broken")
        val preferenceStore = FakePreferenceStore(currentActivePlanId = planId)

        val result = action(
            customPlanStore = FakeCustomPlanStore(deleteFailure = failure),
            preferenceStore = preferenceStore
        ).delete(planId)

        assertTrue(result is CustomDietPlanDeletionResult.Failed)
        assertSame(failure, (result as CustomDietPlanDeletionResult.Failed).cause)
        assertTrue(preferenceStore.removedFavoritePlanIds.isEmpty())
        assertTrue(preferenceStore.clearedActivePlanIds.isEmpty())
    }

    private fun action(
        customPlanStore: FakeCustomPlanStore = FakeCustomPlanStore(),
        preferenceStore: FakePreferenceStore = FakePreferenceStore()
    ): CustomDietPlanDeletionAction {
        return CustomDietPlanDeletionAction(
            customPlanStore = customPlanStore,
            preferenceStore = preferenceStore
        )
    }

    private fun customPlanId(suffix: String): String {
        return "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$suffix"
    }

    private class FakeCustomPlanStore(
        private val deleteFailure: Throwable? = null
    ) : CustomDietPlanDeletionAction.CustomPlanStore {
        val deletedPlanIds = mutableListOf<String>()

        override fun deletePlan(planId: String) {
            deleteFailure?.let { throw it }
            deletedPlanIds += planId
        }
    }

    private class FakePreferenceStore(
        var currentActivePlanId: String? = null
    ) : CustomDietPlanDeletionAction.PreferenceStore {
        val removedFavoritePlanIds = mutableListOf<String>()
        val clearedActivePlanIds = mutableListOf<String>()

        override fun removeFavorite(planId: String) {
            removedFavoritePlanIds += planId
        }

        override fun clearActivePlanIf(planId: String) {
            clearedActivePlanIds += planId

            if (currentActivePlanId == planId) {
                currentActivePlanId = null
            }
        }

        override fun getActivePlanId(): String? {
            return currentActivePlanId
        }
    }
}
