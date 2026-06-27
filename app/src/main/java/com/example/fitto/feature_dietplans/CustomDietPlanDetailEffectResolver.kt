package com.example.fitto.feature_dietplans

import com.example.fitto.R

data class CustomDietPlanDetailEffect(
    val toastMessageRes: Int? = null,
    val toastPlanTitle: String? = null,
    val activePlanId: String? = null,
    val shouldNotifyDietPlanChanged: Boolean = false,
    val shouldRenderSaveState: Boolean = false,
    val shouldRenderApplyState: Boolean = false,
    val shouldCloseScreen: Boolean = false
)

object CustomDietPlanDetailEffectResolver {

    fun favoriteToggled(
        planTitle: String,
        result: DietPlanFavoriteResult
    ): CustomDietPlanDetailEffect {
        return CustomDietPlanDetailEffect(
            toastMessageRes = if (result.isFavorite) {
                R.string.diet_plans_favorite_added_message
            } else {
                R.string.diet_plans_favorite_removed_message
            },
            toastPlanTitle = planTitle,
            activePlanId = result.activePlanId,
            shouldNotifyDietPlanChanged = true,
            shouldRenderSaveState = true
        )
    }

    fun planApplied(
        planTitle: String,
        result: DietPlanApplyResult
    ): CustomDietPlanDetailEffect {
        return CustomDietPlanDetailEffect(
            toastMessageRes = R.string.diet_plans_apply_success,
            toastPlanTitle = planTitle,
            activePlanId = result.activePlanId,
            shouldNotifyDietPlanChanged = true,
            shouldRenderApplyState = true
        )
    }

    fun planDeleted(result: CustomDietPlanDeletionResult): CustomDietPlanDetailEffect {
        return when (result) {
            is CustomDietPlanDeletionResult.Deleted -> CustomDietPlanDetailEffect(
                toastMessageRes = R.string.diet_plans_delete_custom_plan_success,
                activePlanId = result.activePlanId,
                shouldNotifyDietPlanChanged = true,
                shouldCloseScreen = true
            )

            is CustomDietPlanDeletionResult.Failed -> CustomDietPlanDetailEffect(
                toastMessageRes = R.string.diet_plans_delete_custom_plan_error
            )

            CustomDietPlanDeletionResult.NotCustomPlan -> CustomDietPlanDetailEffect()
        }
    }
}
