package com.example.fitto.feature_dietplans

import androidx.annotation.StringRes
import com.example.fitto.R

object DietPlanDetailEffectResolver {

    fun saveButtonState(isSaved: Boolean): DietPlanDetailSaveButtonState {
        return DietPlanDetailSaveButtonState(
            labelRes = if (isSaved) {
                R.string.diet_plan_detail_saved_plan
            } else {
                R.string.diet_plan_detail_save_plan
            },
            alpha = if (isSaved) SAVED_BUTTON_ALPHA else DEFAULT_BUTTON_ALPHA
        )
    }

    fun favoriteToggled(
        planTitle: String,
        result: DietPlanFavoriteResult
    ): DietPlanDetailEffect {
        return DietPlanDetailEffect(
            toastMessageRes = if (result.isFavorite) {
                R.string.diet_plans_favorite_added_message
            } else {
                R.string.diet_plans_favorite_removed_message
            },
            toastPlanTitle = planTitle,
            shouldNotifyDietPlanChanged = true,
            activePlanId = result.activePlanId,
            shouldRenderSaveState = true
        )
    }

    fun planApplied(
        planTitle: String,
        result: DietPlanApplyResult
    ): DietPlanDetailEffect {
        return DietPlanDetailEffect(
            toastMessageRes = R.string.diet_plans_apply_success_message,
            toastPlanTitle = planTitle,
            shouldNotifyDietPlanChanged = true,
            activePlanId = result.activePlanId,
            shouldCloseScreen = true
        )
    }

    private const val DEFAULT_BUTTON_ALPHA = 1f
    private const val SAVED_BUTTON_ALPHA = 0.86f
}

data class DietPlanDetailSaveButtonState(
    @StringRes val labelRes: Int,
    val alpha: Float
)

data class DietPlanDetailEffect(
    @StringRes val toastMessageRes: Int? = null,
    val toastPlanTitle: String? = null,
    val shouldNotifyDietPlanChanged: Boolean = false,
    val activePlanId: String? = null,
    val shouldRenderSaveState: Boolean = false,
    val shouldCloseScreen: Boolean = false
)
