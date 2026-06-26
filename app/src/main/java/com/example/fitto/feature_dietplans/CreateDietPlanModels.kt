package com.example.fitto.feature_dietplans

import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_meal.model.MealType

data class CreateDietPlanFormState(
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

enum class CreatePlanGoal(
    @StringRes val titleRes: Int
) {
    WEIGHT_LOSS(R.string.create_diet_plan_goal_weight_loss),
    WEIGHT_GAIN(R.string.create_diet_plan_goal_weight_gain),
    MUSCLE(R.string.create_diet_plan_goal_muscle),
    MAINTAIN(R.string.create_diet_plan_goal_maintain),
    BALANCED(R.string.create_diet_plan_goal_balanced)
}

enum class CreatePlanPrivacy(
    @StringRes val titleRes: Int
) {
    PRIVATE(R.string.create_diet_plan_private),
    PUBLIC(R.string.create_diet_plan_public)
}

data class CreateDietPlanDefaults(
    val dailyCalories: Int,
    val goal: CreatePlanGoal
)

data class PreviewMealUiModel(
    val mealType: MealType,
    val icon: String,
    val percent: Int,
    val calories: Int
)
