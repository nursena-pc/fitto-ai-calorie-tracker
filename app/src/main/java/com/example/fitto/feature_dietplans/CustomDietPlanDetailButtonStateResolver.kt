package com.example.fitto.feature_dietplans

import com.example.fitto.R

data class CustomDietPlanDetailButtonState(
    val textRes: Int,
    val isEnabled: Boolean = true,
    val alpha: Float = CustomDietPlanDetailButtonStateResolver.ENABLED_ALPHA
)

object CustomDietPlanDetailButtonStateResolver {

    const val ENABLED_ALPHA = 1f
    const val DISABLED_ALPHA = 0.72f

    fun save(isSaved: Boolean): CustomDietPlanDetailButtonState {
        return CustomDietPlanDetailButtonState(
            textRes = if (isSaved) {
                R.string.custom_diet_detail_saved
            } else {
                R.string.diet_plan_detail_save_plan
            }
        )
    }

    fun apply(isActive: Boolean): CustomDietPlanDetailButtonState {
        return CustomDietPlanDetailButtonState(
            textRes = if (isActive) {
                R.string.diet_plans_applied
            } else {
                R.string.diet_plans_apply
            },
            isEnabled = !isActive,
            alpha = if (isActive) DISABLED_ALPHA else ENABLED_ALPHA
        )
    }
}
