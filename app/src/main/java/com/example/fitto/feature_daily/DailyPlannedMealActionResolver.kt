package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_meal.model.MealType

object DailyPlannedMealActionResolver {

    data class ApplyRequest(
        val mealType: MealType,
        val title: String,
        val targetCalories: Int,
        val plannedMealCalories: Int,
        val suggestedFoodNames: List<String>,
        val planId: String,
        val plannedMealId: String,
        @StringRes val mealTypeTitleRes: Int,
        val completion: Completion
    )

    data class Completion(
        val planId: String,
        val dayNumber: Int,
        val plannedMealId: String
    )

    fun resolveApplyRequest(
        dailyPlan: DailyPlannedMealsUiModel?
    ): ApplyRequest? {
        val plan = dailyPlan ?: return null
        val nextMeal = plan.nextMeal ?: return null

        return ApplyRequest(
            mealType = nextMeal.mealType,
            title = nextMeal.title,
            targetCalories = plan.targetCalories,
            plannedMealCalories = nextMeal.scaledCalories(plan.targetCalories),
            suggestedFoodNames = nextMeal.foods,
            planId = plan.planId,
            plannedMealId = nextMeal.id,
            mealTypeTitleRes = nextMeal.mealType.titleRes,
            completion = Completion(
                planId = plan.planId,
                dayNumber = plan.dayNumber,
                plannedMealId = nextMeal.id
            )
        )
    }

    fun resolveCompletionForMealType(
        dailyPlan: DailyPlannedMealsUiModel?,
        mealType: MealType
    ): Completion? {
        val plan = dailyPlan ?: return null
        val nextMeal = plan.nextMeal ?: return null
        if (nextMeal.mealType != mealType) return null

        return Completion(
            planId = plan.planId,
            dayNumber = plan.dayNumber,
            plannedMealId = nextMeal.id
        )
    }
}
