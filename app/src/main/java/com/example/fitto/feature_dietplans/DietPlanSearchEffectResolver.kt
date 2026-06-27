package com.example.fitto.feature_dietplans

import androidx.annotation.StringRes
import com.example.fitto.R

object DietPlanSearchEffectResolver {

    fun favoriteToggled(
        planTitle: String,
        result: DietPlanFavoriteResult
    ): DietPlanSearchEffect {
        return DietPlanSearchEffect(
            toastMessageRes = if (result.isFavorite) {
                R.string.diet_plans_favorite_added_message
            } else {
                R.string.diet_plans_favorite_removed_message
            },
            toastPlanTitle = planTitle,
            shouldReloadPlans = true,
            shouldRefilterPlans = true
        )
    }

    fun planApplied(
        planTitle: String,
        result: DietPlanApplyResult
    ): DietPlanSearchEffect {
        return DietPlanSearchEffect(
            toastMessageRes = R.string.diet_plans_apply_success_message,
            toastPlanTitle = planTitle,
            shouldNotifyDietPlanChanged = true,
            activePlanId = result.activePlanId,
            shouldReloadPlans = true,
            shouldCloseScreen = true
        )
    }

    fun customPlanDeleted(result: CustomDietPlanDeletionResult): DietPlanSearchEffect {
        return when (result) {
            is CustomDietPlanDeletionResult.Deleted -> DietPlanSearchEffect(
                toastMessageRes = R.string.diet_plans_delete_custom_plan_success,
                shouldNotifyDietPlanChanged = true,
                activePlanId = result.activePlanId,
                shouldReloadPlans = true,
                shouldRefilterPlans = true
            )

            is CustomDietPlanDeletionResult.Failed -> DietPlanSearchEffect(
                toastMessageRes = R.string.diet_plans_delete_custom_plan_error
            )

            CustomDietPlanDeletionResult.NotCustomPlan -> DietPlanSearchEffect.None
        }
    }
}

data class DietPlanSearchEffect(
    @StringRes val toastMessageRes: Int? = null,
    val toastPlanTitle: String? = null,
    val shouldNotifyDietPlanChanged: Boolean = false,
    val activePlanId: String? = null,
    val shouldReloadPlans: Boolean = false,
    val shouldRefilterPlans: Boolean = false,
    val shouldCloseScreen: Boolean = false
) {
    companion object {
        val None = DietPlanSearchEffect()
    }
}
