package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel

object DietPlanListSnapshotResolver {

    fun resolve(
        customPlans: List<CustomDietPlanLocalModel>,
        readyPlans: List<DietPlanUiModel>,
        favoriteIds: Set<String>,
        activePlanId: String?,
        customPlanMapper: (CustomDietPlanLocalModel, Boolean) -> DietPlanUiModel
    ): DietPlanListSnapshot {
        val customUiPlans = customPlans.map { customPlan ->
            customPlanMapper(customPlan, customPlan.id in favoriteIds)
                .copy(isApplied = customPlan.id == activePlanId)
        }

        return DietPlanListSnapshot(
            customPlans = customUiPlans,
            readyPlans = readyPlans,
            allPlans = customUiPlans + readyPlans,
            favoriteIds = favoriteIds,
            activePlanId = activePlanId
        )
    }
}
