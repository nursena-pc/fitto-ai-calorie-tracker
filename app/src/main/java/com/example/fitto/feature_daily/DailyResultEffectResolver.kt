package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import java.time.LocalDate

object DailyResultEffectResolver {

    data class DateSelectionEffect(
        val selection: DailyDateSelectionResolver.Selection?
    )

    data class ResultEffect(
        val dateSelectionEffect: DateSelectionEffect,
        val refreshPlan: DailyDashboardRefreshPlan
    )

    data class MealAddedEffect(
        val mealType: MealType,
        val dateSelection: DailyDateSelectionResolver.Selection?,
        val refreshPlan: DailyDashboardRefreshPlan
    )

    fun resolveEntryDate(
        currentDate: LocalDate,
        entryDate: String?
    ): DateSelectionEffect {
        return DateSelectionEffect(
            selection = DailyDateSelectionResolver.resolveEntryDate(
                currentDate = currentDate,
                entryDate = entryDate
            )
        )
    }

    fun resolveDietPlanChanged(): DailyDashboardRefreshPlan {
        return DailyDashboardRefreshPlans.DIET_PLAN_CHANGED
    }

    fun resolveMealAdded(
        currentDate: LocalDate,
        mealTypeArgument: String?,
        entryDate: String?
    ): MealAddedEffect {
        return MealAddedEffect(
            mealType = MealType.fromArgument(mealTypeArgument),
            dateSelection = DailyDateSelectionResolver.resolveEntryDate(
                currentDate = currentDate,
                entryDate = entryDate
            ),
            refreshPlan = DailyDashboardRefreshPlans.MEAL_CHANGED
        )
    }

    fun resolveMealDetailChanged(): DailyDashboardRefreshPlan {
        return DailyDashboardRefreshPlans.MEAL_CHANGED
    }

    fun resolveExerciseAdded(
        currentDate: LocalDate,
        entryDate: String?
    ): ResultEffect {
        return ResultEffect(
            dateSelectionEffect = resolveEntryDate(currentDate, entryDate),
            refreshPlan = DailyDashboardRefreshPlans.EXERCISE_CHANGED
        )
    }
}
