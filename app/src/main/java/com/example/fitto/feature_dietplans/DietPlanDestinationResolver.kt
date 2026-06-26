package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.DietPlanCatalog

object DietPlanDestinationResolver {

    fun resolve(planId: String): DietPlanDestination {
        return when {
            planId == DietPlanCatalog.PLAN_INTERMITTENT_FASTING ->
                DietPlanDestination.IntermittentFasting

            DietPlanDetailCatalog.get(planId) != null ->
                DietPlanDestination.ReadyDetail(planId)

            CustomDietPlanDeletionAction.isCustomPlanId(planId) ->
                DietPlanDestination.CustomDetail(planId)

            else ->
                DietPlanDestination.Unknown(planId)
        }
    }
}

sealed class DietPlanDestination {
    object IntermittentFasting : DietPlanDestination()

    data class ReadyDetail(
        val planId: String
    ) : DietPlanDestination()

    data class CustomDetail(
        val planId: String
    ) : DietPlanDestination()

    data class Unknown(
        val planId: String
    ) : DietPlanDestination()
}
