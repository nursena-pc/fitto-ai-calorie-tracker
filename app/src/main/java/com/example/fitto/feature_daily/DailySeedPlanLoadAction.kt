package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.DietPlanEntity
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.PlannedMealProgressResolver
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel

class DailySeedPlanLoadAction(
    private val getDietPlanById: suspend (planId: String) -> DietPlanEntity?,
    private val getPlannedMealsForDay: suspend (
        planId: String,
        dayNumber: Int
    ) -> List<PlannedMealUiModel>,
    private val getCurrentPlanDay: (durationDays: Int) -> Int,
    private val getCompletedMealIds: (planId: String, dayNumber: Int) -> Set<String>
) {

    suspend fun load(
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ): DailyPlannedMealsUiModel? {
        val roomPlan = getDietPlanById(activePlan.id) ?: return null
        if (roomPlan.durationDays <= 0) return null

        val dayNumber = getCurrentPlanDay(roomPlan.durationDays)
        val plannedMeals = getPlannedMealsForDay(activePlan.id, dayNumber)
        val completedMealIds = getCompletedMealIds(activePlan.id, dayNumber)

        return DailyPlannedMealsUiModel(
            planId = activePlan.id,
            planTitle = roomPlan.nameTr,
            planBadge = activePlan.badge,
            planIcon = roomPlan.icon.ifBlank { activePlan.icon },
            dayNumber = dayNumber,
            durationDays = roomPlan.durationDays,
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
