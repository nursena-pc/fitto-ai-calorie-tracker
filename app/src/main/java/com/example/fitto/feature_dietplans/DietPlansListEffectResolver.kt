package com.example.fitto.feature_dietplans

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.example.fitto.R

object DietPlansListEffectResolver {

    fun favoriteToggled(
        planTitle: String,
        result: DietPlanFavoriteResult
    ): DietPlansListEffect {
        return DietPlansListEffect(
            toastMessageRes = if (result.isFavorite) {
                R.string.diet_plans_favorite_added_message
            } else {
                R.string.diet_plans_favorite_removed_message
            },
            toastPlanTitle = planTitle,
            shouldReloadPlans = true,
            shouldRefreshRecommendation = true
        )
    }

    fun planApplied(
        planTitle: String,
        result: DietPlanApplyResult
    ): DietPlansListEffect {
        return DietPlansListEffect(
            toastMessageRes = R.string.diet_plans_apply_success,
            toastPlanTitle = planTitle,
            shouldNotifyDietPlanChanged = true,
            activePlanId = result.activePlanId,
            shouldReloadPlans = true,
            shouldRefreshRecommendation = true
        )
    }

    fun draftCompleted(): DietPlansListEffect {
        return DietPlansListEffect(
            toastMessageRes = R.string.diet_plans_draft_completed,
            shouldNotifyDietPlanChanged = true,
            selectTab = DietPlansFragment.TAB_MY,
            selectedTab = DietPlanTab.MY,
            checkedChipId = R.id.chipMyPlans,
            shouldReloadPlans = true,
            shouldRefreshRecommendation = true
        )
    }

    fun customPlanDeleted(result: CustomDietPlanDeletionResult): DietPlansListEffect {
        return when (result) {
            is CustomDietPlanDeletionResult.Deleted -> DietPlansListEffect(
                toastMessageRes = R.string.diet_plans_delete_custom_plan_success,
                shouldNotifyDietPlanChanged = true,
                activePlanId = result.activePlanId,
                shouldReloadPlans = true,
                shouldRefreshRecommendation = true
            )

            is CustomDietPlanDeletionResult.Failed -> DietPlansListEffect(
                toastMessageRes = R.string.diet_plans_delete_custom_plan_error
            )

            CustomDietPlanDeletionResult.NotCustomPlan -> DietPlansListEffect.None
        }
    }
}

data class DietPlansListEffect(
    @StringRes val toastMessageRes: Int? = null,
    val toastPlanTitle: String? = null,
    val shouldNotifyDietPlanChanged: Boolean = false,
    val activePlanId: String? = null,
    val selectTab: String? = null,
    val selectedTab: DietPlanTab? = null,
    @IdRes val checkedChipId: Int? = null,
    val shouldReloadPlans: Boolean = false,
    val shouldRefreshRecommendation: Boolean = false
) {
    companion object {
        val None = DietPlansListEffect()
    }
}
