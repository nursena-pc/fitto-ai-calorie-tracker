package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanVisibilityResolverTest {

    @Test
    fun resolve_readyTabShowsOnlyCatalogPlansAndRecommendationCard() {
        val state = DietPlanVisibilityResolver.resolve(
            plans = listOf(
                plan(DietPlanDetailFragment.PLAN_MEDITERRANEAN),
                customPlan("active"),
                customPlan("draft", isDraft = true)
            ),
            selectedTab = DietPlanTab.READY,
            filter = null
        )

        assertEquals(
            listOf(DietPlanDetailFragment.PLAN_MEDITERRANEAN),
            state.visiblePlans.map(DietPlanUiModel::id)
        )
        assertTrue(state.showCreateOwnPlan)
        assertTrue(state.showPersonalizedRecommendation)
        assertEquals(R.string.diet_plans_empty_ready, state.emptyTextRes)
    }

    @Test
    fun resolve_customTabsSplitActivePlansAndDrafts() {
        val activeCustomPlan = customPlan("active")
        val draftCustomPlan = customPlan("draft", isDraft = true)
        val plans = listOf(
            plan(DietPlanDetailFragment.PLAN_DASH),
            activeCustomPlan,
            draftCustomPlan
        )

        val myPlansState = DietPlanVisibilityResolver.resolve(
            plans = plans,
            selectedTab = DietPlanTab.MY,
            filter = null
        )
        val draftsState = DietPlanVisibilityResolver.resolve(
            plans = plans,
            selectedTab = DietPlanTab.DRAFTS,
            filter = null
        )

        assertEquals(listOf(activeCustomPlan.id), myPlansState.visiblePlans.map(DietPlanUiModel::id))
        assertEquals(listOf(draftCustomPlan.id), draftsState.visiblePlans.map(DietPlanUiModel::id))
        assertTrue(myPlansState.showCreateOwnPlan)
        assertFalse(draftsState.showCreateOwnPlan)
        assertFalse(draftsState.showPersonalizedRecommendation)
    }

    @Test
    fun resolve_savedTabKeepsFavoriteNonDraftPlans() {
        val savedReadyPlan = plan(
            id = DietPlanDetailFragment.PLAN_MEDITERRANEAN,
            isFavorite = true
        )
        val savedCustomDraft = customPlan(
            suffix = "draft",
            isFavorite = true,
            isDraft = true
        )

        val state = DietPlanVisibilityResolver.resolve(
            plans = listOf(
                savedReadyPlan,
                plan(DietPlanDetailFragment.PLAN_DASH),
                savedCustomDraft
            ),
            selectedTab = DietPlanTab.SAVED,
            filter = null
        )

        assertEquals(listOf(savedReadyPlan.id), state.visiblePlans.map(DietPlanUiModel::id))
        assertFalse(state.showCreateOwnPlan)
        assertFalse(state.showPersonalizedRecommendation)
        assertEquals(R.string.diet_plans_empty_saved, state.emptyTextRes)
    }

    @Test
    fun resolve_filterNarrowsByCaloriesDurationTypeGoalAndFeatures() {
        val state = DietPlanVisibilityResolver.resolve(
            plans = listOf(
                plan(
                    id = DietPlanDetailFragment.PLAN_DASH,
                    kcalPerDay = 1700,
                    durationDays = 14
                ),
                plan(
                    id = DietPlanDetailFragment.PLAN_KETOGENIC,
                    kcalPerDay = 1700,
                    durationDays = 14
                ),
                plan(
                    id = DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
                    kcalPerDay = 2200,
                    durationDays = 21
                )
            ),
            selectedTab = DietPlanTab.READY,
            filter = DietPlanFilterState(
                goal = DietPlanFilterOptions.GOAL_WEIGHT_LOSS,
                duration = 14,
                type = DietPlanFilterOptions.TYPE_DASH,
                features = setOf(
                    DietPlanFilterOptions.FEATURE_HEART_FRIENDLY,
                    DietPlanFilterOptions.FEATURE_LOW_SODIUM
                ),
                minCalories = 1600,
                maxCalories = 1800
            )
        )

        assertEquals(
            listOf(DietPlanDetailFragment.PLAN_DASH),
            state.visiblePlans.map(DietPlanUiModel::id)
        )
        assertTrue(state.showCreateOwnPlan)
        assertFalse(state.showPersonalizedRecommendation)
        assertEquals(R.string.diet_plans_empty_filtered, state.emptyTextRes)
    }

    private fun plan(
        id: String,
        kcalPerDay: Int = 1800,
        durationDays: Int = 14,
        isFavorite: Boolean = false,
        isDraft: Boolean = false
    ): DietPlanUiModel {
        return DietPlanUiModel(
            id = id,
            titleText = id,
            kcalPerDay = kcalPerDay,
            durationDays = durationDays,
            isFavorite = isFavorite,
            isDraft = isDraft
        )
    }

    private fun customPlan(
        suffix: String,
        isFavorite: Boolean = false,
        isDraft: Boolean = false
    ): DietPlanUiModel {
        return plan(
            id = "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$suffix",
            isFavorite = isFavorite,
            isDraft = isDraft
        )
    }
}
