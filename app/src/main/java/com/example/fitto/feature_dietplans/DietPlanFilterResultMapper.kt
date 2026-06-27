package com.example.fitto.feature_dietplans

object DietPlanFilterResultMapper {
    fun fromResult(
        clearFilters: Boolean,
        goal: String?,
        duration: Int,
        type: String?,
        features: Collection<String>?,
        minCalories: Int,
        maxCalories: Int
    ): DietPlanFilterState? {
        if (clearFilters) return null

        return DietPlanFilterState(
            goal = goal.orEmpty(),
            duration = duration,
            type = type.orEmpty(),
            features = features.orEmpty().toSet(),
            minCalories = minCalories,
            maxCalories = maxCalories
        )
    }
}
