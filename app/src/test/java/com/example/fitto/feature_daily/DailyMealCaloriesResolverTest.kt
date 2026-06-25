package com.example.fitto.feature_daily

import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyMealCaloriesResolverTest {

    @Test
    fun resolve_returnsCaloriesForEachDailyMealType() {
        assertEquals(
            DailyMealCaloriesResolver.Calories(
                breakfast = 320,
                snack = 150,
                lunch = 640,
                dinner = 520
            ),
            DailyMealCaloriesResolver.resolve(
                listOf(
                    summary(MealType.BREAKFAST, calories = 320),
                    summary(MealType.SNACK, calories = 150),
                    summary(MealType.LUNCH, calories = 640),
                    summary(MealType.DINNER, calories = 520)
                )
            )
        )
    }

    @Test
    fun resolve_usesZeroForMissingMealTypes() {
        assertEquals(
            DailyMealCaloriesResolver.Calories(
                breakfast = 0,
                snack = 0,
                lunch = 640,
                dinner = 0
            ),
            DailyMealCaloriesResolver.resolve(
                listOf(summary(MealType.LUNCH, calories = 640))
            )
        )
    }

    private fun summary(
        mealType: MealType,
        calories: Int
    ): DailyMealSummary {
        return DailyMealSummary(
            mealType = mealType.argumentValue,
            totalCalories = calories,
            totalProtein = 0.0,
            totalCarbs = 0.0,
            totalFat = 0.0
        )
    }
}
