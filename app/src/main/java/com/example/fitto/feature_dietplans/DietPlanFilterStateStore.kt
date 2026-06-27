package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.DietPlanFilterOptions as FilterOptions

class DietPlanFilterStateStore(
    initialState: DietPlanFilterState = defaultState()
) {
    private var state = initialState.normalized()

    fun current(): DietPlanFilterState = state

    fun selectGoal(goal: String) {
        state = state.copy(goal = goal)
    }

    fun selectDuration(duration: Int) {
        state = state.copy(duration = duration)
    }

    fun selectType(type: String) {
        state = state.copy(type = type)
    }

    fun setCalorieRange(minCalories: Int, maxCalories: Int) {
        state = state.copy(
            minCalories = minCalories,
            maxCalories = maxCalories
        )
    }

    fun toggleFeature(feature: String): Boolean {
        val nextFeatures = state.features.toMutableSet()
        val willSelect = feature !in nextFeatures

        if (willSelect) {
            nextFeatures.add(feature)
        } else {
            nextFeatures.remove(feature)
        }

        state = state.copy(features = nextFeatures)
        return willSelect
    }

    fun reset(): DietPlanFilterState {
        state = defaultState()
        return state
    }

    private fun DietPlanFilterState.normalized(): DietPlanFilterState {
        return copy(features = features.toMutableSet())
    }

    companion object {
        fun defaultState(): DietPlanFilterState {
            return DietPlanFilterState(
                goal = FilterOptions.DEFAULT_GOAL,
                duration = FilterOptions.DEFAULT_DURATION,
                type = FilterOptions.DEFAULT_TYPE,
                features = FilterOptions.DEFAULT_FEATURES.toMutableSet(),
                minCalories = FilterOptions.DEFAULT_MIN_CALORIES,
                maxCalories = FilterOptions.DEFAULT_MAX_CALORIES
            )
        }
    }
}
