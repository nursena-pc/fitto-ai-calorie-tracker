package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R

object DailyPlannedMealApplyResultResolver {

    data class SuccessEffect(
        @StringRes val messageRes: Int,
        @StringRes val mealTypeTitleRes: Int,
        val refreshMealTotals: Boolean,
        val refreshActivePlan: Boolean
    )

    data class FailureEffect(
        @StringRes val messageRes: Int
    )

    fun resolveSuccess(
        request: DailyPlannedMealActionResolver.ApplyRequest
    ): SuccessEffect {
        return SuccessEffect(
            messageRes = R.string.daily_planned_meal_applied_message,
            mealTypeTitleRes = request.mealTypeTitleRes,
            refreshMealTotals = true,
            refreshActivePlan = true
        )
    }

    fun resolveFailure(): FailureEffect {
        return FailureEffect(
            messageRes = R.string.daily_planned_meal_save_error_message
        )
    }
}
