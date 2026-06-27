package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import kotlin.math.roundToInt

data class CustomDietPlanDetailRenderModel(
    val title: String,
    val description: String,
    val goal: CreatePlanGoal,
    val dailyCalories: Int,
    val mealCount: Int,
    val durationDays: Int,
    val macroTargets: CustomDietPlanMacroTargets,
    val preferences: List<String>,
    val exclusions: List<String>,
    val mealOrder: List<String>
)

data class CustomDietPlanMacroTargets(
    val carbGrams: Int,
    val proteinGrams: Int,
    val fatGrams: Int
)

object CustomDietPlanDetailMapper {

    fun map(plan: CustomDietPlanLocalModel): CustomDietPlanDetailRenderModel {
        return CustomDietPlanDetailRenderModel(
            title = plan.title,
            description = plan.description,
            goal = goalFrom(plan.goalType),
            dailyCalories = plan.dailyCalories,
            mealCount = plan.mealCount,
            durationDays = plan.durationDays,
            macroTargets = macroTargetsFor(plan.dailyCalories),
            preferences = plan.preferences,
            exclusions = plan.exclusions,
            mealOrder = plan.mealOrder
        )
    }

    fun goalFrom(goalType: String): CreatePlanGoal {
        return runCatching {
            CreatePlanGoal.valueOf(goalType.uppercase())
        }.getOrDefault(CreatePlanGoal.BALANCED)
    }

    fun macroTargetsFor(dailyCalories: Int): CustomDietPlanMacroTargets {
        return CustomDietPlanMacroTargets(
            carbGrams = ((dailyCalories * 0.45) / 4.0).roundToInt(),
            proteinGrams = ((dailyCalories * 0.25) / 4.0).roundToInt(),
            fatGrams = ((dailyCalories * 0.30) / 9.0).roundToInt()
        )
    }
}
