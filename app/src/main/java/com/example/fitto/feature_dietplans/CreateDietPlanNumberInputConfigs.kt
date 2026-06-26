package com.example.fitto.feature_dietplans

import com.example.fitto.R

object CreateDietPlanNumberInputConfigs {

    fun calories(initialValue: Int): CreateDietPlanNumberInputConfig {
        return CreateDietPlanNumberInputConfig(
            icon = CALORIE_ICON,
            titleRes = R.string.create_diet_plan_calorie_dialog_title,
            helperTextRes = R.string.create_diet_plan_calorie_dialog_helper,
            unitTextRes = R.string.create_diet_plan_calorie_dialog_unit,
            initialValue = initialValue,
            validRange = CreateDietPlanNumberRules.CALORIE_RANGE,
            step = CreateDietPlanNumberRules.CALORIE_STEP,
            errorTextRes = R.string.create_diet_plan_calorie_dialog_error
        )
    }

    fun duration(initialValue: Int): CreateDietPlanNumberInputConfig {
        return CreateDietPlanNumberInputConfig(
            icon = DURATION_ICON,
            titleRes = R.string.create_diet_plan_duration_dialog_title,
            helperTextRes = R.string.create_diet_plan_duration_dialog_helper,
            unitTextRes = R.string.create_diet_plan_duration_dialog_unit,
            initialValue = initialValue,
            validRange = CreateDietPlanNumberRules.DURATION_RANGE,
            step = CreateDietPlanNumberRules.DURATION_STEP,
            errorTextRes = R.string.create_diet_plan_duration_dialog_error
        )
    }

    private const val CALORIE_ICON = "\uD83D\uDD25"
    private const val DURATION_ICON = "\uD83D\uDCC5"
}
