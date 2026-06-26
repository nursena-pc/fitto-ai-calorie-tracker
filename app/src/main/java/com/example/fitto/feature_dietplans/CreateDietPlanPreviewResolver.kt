package com.example.fitto.feature_dietplans

import com.example.fitto.feature_meal.model.MealType

object CreateDietPlanPreviewResolver {

    fun buildPreviewMeals(
        selectedMealTypes: Collection<MealType>,
        dailyCalories: Int
    ): Map<MealType, PreviewMealUiModel> {
        val selectedMeals = selectedMealTypes.toList()
        val totalRatio = selectedMeals.sumOf(::mealBaseRatio)

        return selectedMeals.associateWith { mealType ->
            val ratio = mealBaseRatio(mealType) / totalRatio.coerceAtLeast(MIN_TOTAL_RATIO)
            PreviewMealUiModel(
                mealType = mealType,
                icon = mealIcon(mealType),
                percent = (ratio * PERCENT_MULTIPLIER).toInt(),
                calories = (dailyCalories * ratio).toInt()
            )
        }
    }

    fun mealBaseRatio(mealType: MealType): Double {
        return when (mealType) {
            MealType.BREAKFAST -> 0.25
            MealType.SNACK -> 0.15
            MealType.LUNCH -> 0.35
            MealType.DINNER -> 0.25
        }
    }

    fun mealIcon(mealType: MealType): String {
        return when (mealType) {
            MealType.BREAKFAST -> "☀️"
            MealType.SNACK -> "🥜"
            MealType.LUNCH -> "🍽️"
            MealType.DINNER -> "🌙"
        }
    }

    private const val MIN_TOTAL_RATIO = 0.01
    private const val PERCENT_MULTIPLIER = 100
}
