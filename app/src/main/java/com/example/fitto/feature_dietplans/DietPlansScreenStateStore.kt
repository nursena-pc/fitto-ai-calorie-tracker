package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.DietPlanUiModel

data class DietPlansScreenState(
    val selectedTab: DietPlanTab = DietPlanTab.READY,
    val activeFilter: DietPlanFilterState? = null,
    val plans: List<DietPlanUiModel> = emptyList(),
    val recommendation: PersonalizedDietRecommendation? = null,
    val recommendationGeneration: Long = 0L
)

class DietPlansScreenStateStore(
    initialState: DietPlansScreenState = DietPlansScreenState()
) {
    var state: DietPlansScreenState = initialState
        private set

    fun selectTab(tab: DietPlanTab) {
        state = state.copy(selectedTab = tab)
    }

    fun setFilter(filter: DietPlanFilterState?) {
        state = state.copy(activeFilter = filter)
    }

    fun setPlans(plans: List<DietPlanUiModel>) {
        state = state.copy(plans = plans)
    }

    fun visibleState(): DietPlanVisibilityResolver.State {
        return DietPlanVisibilityResolver.resolve(
            plans = state.plans,
            selectedTab = state.selectedTab,
            filter = state.activeFilter
        )
    }

    fun beginRecommendationLoad(): Long {
        val generation = state.recommendationGeneration + 1L
        state = state.copy(recommendationGeneration = generation)
        return generation
    }

    fun applyRecommendation(
        generation: Long,
        recommendation: PersonalizedDietRecommendation
    ): Boolean {
        if (generation != state.recommendationGeneration) return false
        state = state.copy(recommendation = recommendation)
        return true
    }
}
