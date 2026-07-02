package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.data.DietPlanCatalog

sealed interface DailyActivePlanDestination {
    data object DietPlans : DailyActivePlanDestination
    data object IntermittentFastingDetail : DailyActivePlanDestination
    data class DietPlanDetail(val planId: String) : DailyActivePlanDestination
}

object DailyActivePlanDestinationResolver {
    fun resolve(planId: String): DailyActivePlanDestination {
        return when {
            DailyActivePlanPolicy.isCustomPlan(planId) -> DailyActivePlanDestination.DietPlans
            planId == DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> {
                DailyActivePlanDestination.IntermittentFastingDetail
            }
            else -> DailyActivePlanDestination.DietPlanDetail(planId)
        }
    }
}
