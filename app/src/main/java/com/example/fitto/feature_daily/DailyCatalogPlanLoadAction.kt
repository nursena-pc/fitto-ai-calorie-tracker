package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.PlannedMealProgressResolver
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel

class DailyCatalogPlanLoadAction(
    private val getPlanDurationDays: (planId: String) -> Int,
    private val getCurrentPlanDay: (durationDays: Int) -> Int,
    private val getMealsForDay: (planId: String, dayNumber: Int) -> List<PlannedMealUiModel>,
    private val getCompletedMealIds: (planId: String, dayNumber: Int) -> Set<String>,
    private val getPlanTitle: (planId: String) -> String,
    private val getPlanBadge: (planId: String) -> String,
    private val getPlanIcon: (planId: String) -> String
) {

    fun load(
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ): DailyPlannedMealsUiModel? {
        val durationDays = getPlanDurationDays(activePlan.id)
        if (durationDays <= 0) return null

        val dayNumber = getCurrentPlanDay(durationDays)
        val plannedMeals = getMealsForDay(activePlan.id, dayNumber)
        if (plannedMeals.isEmpty()) return null

        val completedMealIds = getCompletedMealIds(activePlan.id, dayNumber)

        return DailyPlannedMealsUiModel(
            planId = activePlan.id,
            planTitle = getPlanTitle(activePlan.id),
            planBadge = getPlanBadge(activePlan.id),
            planIcon = getPlanIcon(activePlan.id),
            dayNumber = dayNumber,
            durationDays = durationDays,
            targetCalories = targetCalories,
            meals = plannedMeals,
            nextMeal = PlannedMealProgressResolver.nextMeal(
                meals = plannedMeals,
                completedMealIds = completedMealIds
            ),
            completedMealIds = completedMealIds
        )
    }
}
