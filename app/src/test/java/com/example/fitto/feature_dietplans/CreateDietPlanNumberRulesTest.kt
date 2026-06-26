package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanNumberRulesTest {

    @Test
    fun calorieAdjustments_useConfiguredStepAndStayInRange() {
        assertEquals(1750, CreateDietPlanNumberRules.decreaseCalories(1800))
        assertEquals(1850, CreateDietPlanNumberRules.increaseCalories(1800))
        assertEquals(800, CreateDietPlanNumberRules.decreaseCalories(800))
        assertEquals(5000, CreateDietPlanNumberRules.increaseCalories(5000))
    }

    @Test
    fun durationAdjustments_useConfiguredStepAndStayInRange() {
        assertEquals(29, CreateDietPlanNumberRules.decreaseDuration(30))
        assertEquals(31, CreateDietPlanNumberRules.increaseDuration(30))
        assertEquals(1, CreateDietPlanNumberRules.decreaseDuration(1))
        assertEquals(365, CreateDietPlanNumberRules.increaseDuration(365))
    }

    @Test
    fun calorieDialogConfig_usesSharedRulesAndResources() {
        val config = CreateDietPlanNumberInputConfigs.calories(initialValue = 1800)

        assertEquals(R.string.create_diet_plan_calorie_dialog_title, config.titleRes)
        assertEquals(R.string.create_diet_plan_calorie_dialog_helper, config.helperTextRes)
        assertEquals(R.string.create_diet_plan_calorie_dialog_unit, config.unitTextRes)
        assertEquals(1800, config.initialValue)
        assertEquals(CreateDietPlanNumberRules.CALORIE_RANGE, config.validRange)
        assertEquals(CreateDietPlanNumberRules.CALORIE_STEP, config.step)
        assertEquals(R.string.create_diet_plan_calorie_dialog_error, config.errorTextRes)
    }

    @Test
    fun durationDialogConfig_usesSharedRulesAndResources() {
        val config = CreateDietPlanNumberInputConfigs.duration(initialValue = 30)

        assertEquals(R.string.create_diet_plan_duration_dialog_title, config.titleRes)
        assertEquals(R.string.create_diet_plan_duration_dialog_helper, config.helperTextRes)
        assertEquals(R.string.create_diet_plan_duration_dialog_unit, config.unitTextRes)
        assertEquals(30, config.initialValue)
        assertEquals(CreateDietPlanNumberRules.DURATION_RANGE, config.validRange)
        assertEquals(CreateDietPlanNumberRules.DURATION_STEP, config.step)
        assertEquals(R.string.create_diet_plan_duration_dialog_error, config.errorTextRes)
    }
}
