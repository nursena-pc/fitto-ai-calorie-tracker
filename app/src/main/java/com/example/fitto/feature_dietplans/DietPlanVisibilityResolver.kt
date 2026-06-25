package com.example.fitto.feature_dietplans

import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.model.DietPlanUiModel

object DietPlanVisibilityResolver {

    data class State(
        val visiblePlans: List<DietPlanUiModel>,
        val showCreateOwnPlan: Boolean,
        val showPersonalizedRecommendation: Boolean,
        @StringRes val emptyTextRes: Int
    )

    fun resolve(
        plans: List<DietPlanUiModel>,
        selectedTab: DietPlanTab,
        filter: DietPlanFilterState?
    ): State {
        val tabPlans = plans.filterForTab(selectedTab)
        val visiblePlans = filter?.let { activeFilter ->
            tabPlans.filter { plan -> plan.matchesFilter(activeFilter) }
        } ?: tabPlans

        return State(
            visiblePlans = visiblePlans,
            showCreateOwnPlan = selectedTab == DietPlanTab.READY || selectedTab == DietPlanTab.MY,
            showPersonalizedRecommendation = selectedTab == DietPlanTab.READY && filter == null,
            emptyTextRes = resolveEmptyTextRes(
                selectedTab = selectedTab,
                hasFilter = filter != null
            )
        )
    }

    fun isCustomPlan(planId: String): Boolean {
        return planId.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)
    }

    private fun List<DietPlanUiModel>.filterForTab(
        selectedTab: DietPlanTab
    ): List<DietPlanUiModel> {
        return when (selectedTab) {
            DietPlanTab.READY -> filterNot { plan -> isCustomPlan(plan.id) }
            DietPlanTab.MY -> filter { plan -> isCustomPlan(plan.id) && !plan.isDraft }
            DietPlanTab.DRAFTS -> filter { plan -> isCustomPlan(plan.id) && plan.isDraft }
            DietPlanTab.SAVED -> filter { plan -> plan.isFavorite && !plan.isDraft }
        }
    }

    private fun resolveEmptyTextRes(
        selectedTab: DietPlanTab,
        hasFilter: Boolean
    ): Int {
        if (hasFilter) return R.string.diet_plans_empty_filtered

        return when (selectedTab) {
            DietPlanTab.MY -> R.string.diet_plans_empty_my_plans
            DietPlanTab.DRAFTS -> R.string.diet_plans_empty_drafts
            DietPlanTab.SAVED -> R.string.diet_plans_empty_saved
            DietPlanTab.READY -> R.string.diet_plans_empty_ready
        }
    }

    private fun DietPlanUiModel.matchesFilter(filter: DietPlanFilterState): Boolean {
        if (kcalPerDay !in filter.minCalories..filter.maxCalories) return false
        if (filter.duration > 0 && durationDays != filter.duration) return false
        if (filter.type.isNotBlank() && planTypeKey() != filter.type) return false
        if (filter.goal.isNotBlank() && !matchesGoal(filter.goal)) return false

        if (filter.features.isNotEmpty() && !filter.features.all { feature -> matchesFeature(feature) }) {
            return false
        }

        return true
    }

    private fun DietPlanUiModel.planTypeKey(): String {
        return when (id) {
            DietPlanDetailFragment.PLAN_MEDITERRANEAN -> "mediterranean"
            DietPlanDetailFragment.PLAN_DASH -> "dash"
            DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> "intermittent_fasting"
            DietPlanDetailFragment.PLAN_KETOGENIC -> "ketogenic"
            DietPlanDetailFragment.PLAN_PLANT_BASED -> "plant_based"
            else -> if (isCustomPlan(id)) "custom" else id
        }
    }

    private fun DietPlanUiModel.matchesGoal(goal: String): Boolean {
        return when (goal) {
            "weight_loss" -> id != DietPlanDetailFragment.PLAN_WEIGHT_GAIN
            "weight_gain" -> id == DietPlanDetailFragment.PLAN_WEIGHT_GAIN || isCustomPlan(id)
            "muscle_gain" -> id == DietPlanDetailFragment.PLAN_WEIGHT_GAIN ||
                id == DietPlanDetailFragment.PLAN_KETOGENIC ||
                isCustomPlan(id)
            "balanced" -> true
            else -> true
        }
    }

    private fun DietPlanUiModel.matchesFeature(feature: String): Boolean {
        return when (feature) {
            "heart_friendly" -> id == DietPlanDetailFragment.PLAN_MEDITERRANEAN ||
                id == DietPlanDetailFragment.PLAN_DASH ||
                id == DietPlanDetailFragment.PLAN_MIND ||
                id == DietPlanDetailFragment.PLAN_PLANT_BASED

            "low_sodium" -> id == DietPlanDetailFragment.PLAN_DASH

            "high_protein" -> id == DietPlanDetailFragment.PLAN_KETOGENIC ||
                id == DietPlanDetailFragment.PLAN_WEIGHT_GAIN ||
                isCustomPlan(id)

            "easy" -> durationDays <= 14 ||
                id == DietPlanDetailFragment.PLAN_MEDITERRANEAN

            else -> true
        }
    }
}
