package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType

data class CreateDietPlanEditableState(
    val planName: String,
    val description: String,
    val goal: CreatePlanGoal,
    val privacy: CreatePlanPrivacy,
    val dailyCalories: Int,
    val durationDays: Int,
    val mealTypes: List<MealType>,
    val preferences: List<String>,
    val exclusions: List<String>
)

object CreateDietPlanDraftMapper {

    fun toEditableState(plan: CustomDietPlanLocalModel): CreateDietPlanEditableState {
        return CreateDietPlanEditableState(
            planName = plan.title,
            description = plan.description,
            goal = goalFor(plan.goalType),
            privacy = privacyFor(plan.isPublic),
            dailyCalories = plan.dailyCalories,
            durationDays = plan.durationDays,
            mealTypes = mealTypesFor(plan.mealOrder),
            preferences = plan.preferences,
            exclusions = plan.exclusions
        )
    }

    private fun goalFor(goalType: String): CreatePlanGoal {
        return runCatching {
            CreatePlanGoal.valueOf(goalType)
        }.getOrDefault(CreatePlanGoal.BALANCED)
    }

    private fun privacyFor(isPublic: Boolean): CreatePlanPrivacy {
        return if (isPublic) {
            CreatePlanPrivacy.PUBLIC
        } else {
            CreatePlanPrivacy.PRIVATE
        }
    }

    private fun mealTypesFor(mealOrder: List<String>): List<MealType> {
        val mealTypes = mealOrder.mapNotNull { mealKey ->
            MealType.entries.firstOrNull { it.argumentValue == mealKey }
        }

        return mealTypes.ifEmpty { DEFAULT_MEAL_TYPES }
    }

    private val DEFAULT_MEAL_TYPES = listOf(
        MealType.BREAKFAST,
        MealType.SNACK,
        MealType.LUNCH,
        MealType.DINNER
    )
}
