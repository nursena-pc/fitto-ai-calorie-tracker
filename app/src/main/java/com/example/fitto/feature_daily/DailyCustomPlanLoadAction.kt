package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel

class DailyCustomPlanLoadAction(
    private val getAllFoods: suspend () -> List<FoodEntity>,
    private val getCurrentPlanDay: (durationDays: Int) -> Int,
    private val getCompletedMealIds: (planId: String, dayNumber: Int) -> Set<String>,
    private val createActivePlan: (CustomDietPlanLocalModel) -> ActiveDietPlanUiModel,
    private val buildDailyPlan: (
        plan: CustomDietPlanLocalModel,
        allFoods: List<FoodEntity>,
        dayNumber: Int,
        completedMealIds: Set<String>
    ) -> DailyPlannedMealsUiModel
) {

    suspend fun load(plan: CustomDietPlanLocalModel): DailyCustomPlanLoadResult {
        val activePlan = createActivePlan(plan)
        val allFoods = getAllFoods()
        val dayNumber = getCurrentPlanDay(plan.durationDays)
        val completedMealIds = getCompletedMealIds(plan.id, dayNumber)
        val dailyPlan = buildDailyPlan(
            plan,
            allFoods,
            dayNumber,
            completedMealIds
        )

        return DailyCustomPlanLoadResult(
            activePlan = activePlan,
            dailyPlan = dailyPlan
        )
    }
}

data class DailyCustomPlanLoadResult(
    val activePlan: ActiveDietPlanUiModel,
    val dailyPlan: DailyPlannedMealsUiModel
)
