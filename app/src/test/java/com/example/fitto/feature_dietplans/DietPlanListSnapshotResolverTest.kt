package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanListSnapshotResolverTest {

    @Test
    fun resolve_mapsCustomPlanFavoriteAndActiveState() {
        val activePlan = customPlan("active")
        val draftPlan = customPlan("draft", isDraft = true)
        val readyPlan = readyPlan("ready_saved")

        val snapshot = DietPlanListSnapshotResolver.resolve(
            customPlans = listOf(activePlan, draftPlan),
            readyPlans = listOf(readyPlan),
            favoriteIds = setOf(activePlan.id, readyPlan.id),
            activePlanId = activePlan.id,
            customPlanMapper = ::mapCustomPlan
        )

        assertEquals(
            listOf(activePlan.id, draftPlan.id, readyPlan.id),
            snapshot.allPlans.map(DietPlanUiModel::id)
        )
        assertEquals(listOf(activePlan.id, draftPlan.id), snapshot.customPlans.map(DietPlanUiModel::id))
        assertEquals(listOf(readyPlan.id), snapshot.readyPlans.map(DietPlanUiModel::id))
        assertTrue(snapshot.customPlans.first().isFavorite)
        assertTrue(snapshot.customPlans.first().isApplied)
        assertFalse(snapshot.customPlans.last().isFavorite)
        assertFalse(snapshot.customPlans.last().isApplied)
        assertEquals(setOf(activePlan.id, readyPlan.id), snapshot.favoriteIds)
        assertEquals(activePlan.id, snapshot.activePlanId)
    }

    @Test
    fun resolve_preservesCustomBeforeReadyOrdering() {
        val firstCustom = customPlan("first")
        val secondCustom = customPlan("second")
        val firstReady = readyPlan("ready_first")
        val secondReady = readyPlan("ready_second")

        val snapshot = DietPlanListSnapshotResolver.resolve(
            customPlans = listOf(firstCustom, secondCustom),
            readyPlans = listOf(firstReady, secondReady),
            favoriteIds = emptySet(),
            activePlanId = null,
            customPlanMapper = ::mapCustomPlan
        )

        assertEquals(
            listOf(firstCustom.id, secondCustom.id, firstReady.id, secondReady.id),
            snapshot.allPlans.map(DietPlanUiModel::id)
        )
    }

    private fun mapCustomPlan(
        plan: CustomDietPlanLocalModel,
        isFavorite: Boolean
    ): DietPlanUiModel {
        return DietPlanUiModel(
            id = plan.id,
            titleText = plan.title,
            kcalPerDay = plan.dailyCalories,
            durationDays = plan.durationDays,
            isFavorite = isFavorite,
            isDraft = plan.isDraft
        )
    }

    private fun customPlan(
        suffix: String,
        isDraft: Boolean = false
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$suffix",
            title = "Plan $suffix",
            description = "",
            goalType = "BALANCED",
            dailyCalories = 1800,
            mealCount = 4,
            durationDays = 14,
            isPublic = false,
            preferences = emptyList(),
            exclusions = emptyList(),
            mealOrder = emptyList(),
            isDraft = isDraft,
            createdAt = 0L,
            updatedAt = 0L
        )
    }

    private fun readyPlan(id: String): DietPlanUiModel {
        return DietPlanUiModel(
            id = id,
            titleText = id,
            kcalPerDay = 1700,
            durationDays = 14
        )
    }
}
