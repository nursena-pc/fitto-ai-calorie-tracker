package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel

object DailyPlannedMealUiStateResolver {

    data class State(
        val planTitle: String,
        val planIcon: String,
        val dayNumber: Int,
        val durationDays: Int,
        val targetCalories: Int,
        val completedMealCount: Int,
        val totalMealCount: Int,
        val progressPercent: Int,
        val nextMealState: NextMealState
    )

    sealed interface NextMealState {
        data object Completed : NextMealState

        data class Available(
            @StringRes val mealTypeTitleRes: Int,
            val calories: Int,
            val mealTitle: String,
            val foodSummary: String
        ) : NextMealState
    }

    fun resolve(plan: DailyPlannedMealsUiModel): State {
        return State(
            planTitle = plan.planTitle,
            planIcon = plan.planIcon,
            dayNumber = plan.dayNumber,
            durationDays = plan.durationDays,
            targetCalories = plan.targetCalories,
            completedMealCount = plan.completedMealIds.size,
            totalMealCount = plan.meals.size,
            progressPercent = plan.progressPercent,
            nextMealState = resolveNextMealState(plan)
        )
    }

    private fun resolveNextMealState(
        plan: DailyPlannedMealsUiModel
    ): NextMealState {
        val nextMeal = plan.nextMeal ?: return NextMealState.Completed

        return NextMealState.Available(
            mealTypeTitleRes = nextMeal.mealType.titleRes,
            calories = nextMeal.scaledCalories(plan.targetCalories),
            mealTitle = nextMeal.title,
            foodSummary = nextMeal.scaledFoodSummary(plan.targetCalories)
        )
    }
}
