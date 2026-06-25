package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.feature_meal.model.MealType

object DailyMealCaloriesResolver {

    data class Calories(
        val breakfast: Int,
        val snack: Int,
        val lunch: Int,
        val dinner: Int
    )

    fun resolve(mealSummaries: List<DailyMealSummary>): Calories {
        val summariesByType = mealSummaries.associateBy { summary ->
            summary.mealType
        }

        return Calories(
            breakfast = summariesByType.caloriesFor(MealType.BREAKFAST),
            snack = summariesByType.caloriesFor(MealType.SNACK),
            lunch = summariesByType.caloriesFor(MealType.LUNCH),
            dinner = summariesByType.caloriesFor(MealType.DINNER)
        )
    }

    private fun Map<String, DailyMealSummary>.caloriesFor(mealType: MealType): Int {
        return this[mealType.argumentValue]?.totalCalories ?: 0
    }
}
