package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import java.time.LocalDate

object DailyResultEffectResolver {

    data class DateSelectionEffect(
        val selection: DailyDateSelectionResolver.Selection?
    )

    data class RefreshEffect(
        val shouldLoadHealthConnectSummary: Boolean = false,
        val shouldRenderMealTotals: Boolean = false,
        val shouldRenderActivePlan: Boolean = false,
        val shouldRenderWeeklyCalorieProgress: Boolean = false
    )

    data class MealAddedEffect(
        val mealType: MealType,
        val dateSelection: DailyDateSelectionResolver.Selection?,
        val refreshEffect: RefreshEffect
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

    fun resolveDietPlanChanged(): RefreshEffect {
        return RefreshEffect(
            shouldRenderMealTotals = true,
            shouldRenderActivePlan = true
        )
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
            refreshEffect = RefreshEffect(
                shouldRenderMealTotals = true,
                shouldRenderActivePlan = true,
                shouldRenderWeeklyCalorieProgress = true
            )
        )
    }

    fun resolveMealDetailChanged(): RefreshEffect {
        return RefreshEffect(
            shouldRenderMealTotals = true,
            shouldRenderActivePlan = true,
            shouldRenderWeeklyCalorieProgress = true
        )
    }

    fun resolveExerciseAdded(
        currentDate: LocalDate,
        entryDate: String?
    ): Pair<DateSelectionEffect, RefreshEffect> {
        return resolveEntryDate(currentDate, entryDate) to RefreshEffect(
            shouldLoadHealthConnectSummary = true,
            shouldRenderMealTotals = true,
            shouldRenderActivePlan = true
        )
    }
}
