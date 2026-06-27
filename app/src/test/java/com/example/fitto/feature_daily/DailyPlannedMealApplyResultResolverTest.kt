package com.example.fitto.feature_daily

import com.example.fitto.R
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPlannedMealApplyResultResolverTest {

    @Test
    fun resolveSuccess_returnsToastAndRefreshEffects() {
        val effect = DailyPlannedMealApplyResultResolver.resolveSuccess(
            request = applyRequest(mealTypeTitleRes = R.string.daily_dinner)
        )

        assertEquals(R.string.daily_planned_meal_applied_message, effect.messageRes)
        assertEquals(R.string.daily_dinner, effect.mealTypeTitleRes)
        assertTrue(effect.refreshMealTotals)
        assertTrue(effect.refreshActivePlan)
    }

    @Test
    fun resolveFailure_returnsSaveErrorToast() {
        val effect = DailyPlannedMealApplyResultResolver.resolveFailure()

        assertEquals(R.string.daily_planned_meal_save_error_message, effect.messageRes)
    }

    private fun applyRequest(
        mealTypeTitleRes: Int
    ): DailyPlannedMealActionResolver.ApplyRequest {
        return DailyPlannedMealActionResolver.ApplyRequest(
            mealType = MealType.DINNER,
            title = "Dinner",
            targetCalories = 1800,
            plannedMealCalories = 550,
            suggestedFoodNames = listOf("Soup"),
            planId = "balanced",
            plannedMealId = "dinner",
            mealTypeTitleRes = mealTypeTitleRes,
            completion = DailyPlannedMealActionResolver.Completion(
                planId = "balanced",
                dayNumber = 1,
                plannedMealId = "dinner"
            )
        )
    }
}
