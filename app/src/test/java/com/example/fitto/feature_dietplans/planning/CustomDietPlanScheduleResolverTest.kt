package com.example.fitto.feature_dietplans.planning

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomDietPlanScheduleResolverTest {

    @Test
    fun normalizedDay_wrapsDayInsidePlanDuration() {
        assertEquals(1, CustomDietPlanScheduleResolver.normalizedDay(dayNumber = 8, durationDays = 7))
        assertEquals(7, CustomDietPlanScheduleResolver.normalizedDay(dayNumber = 0, durationDays = 7))
        assertEquals(1, CustomDietPlanScheduleResolver.normalizedDay(dayNumber = 3, durationDays = 0))
    }

    @Test
    fun mealTypesFor_usesDistinctValidMealOrderBeforeMealCountFallback() {
        val mealTypes = CustomDietPlanScheduleResolver.mealTypesFor(
            mealOrder = listOf("akşam", "breakfast", "akşam", "unknown"),
            mealCount = 4
        )

        assertEquals(listOf(MealType.DINNER, MealType.BREAKFAST), mealTypes)
    }

    @Test
    fun mealTypesFor_fallsBackToMealCountWhenMealOrderIsEmpty() {
        assertEquals(
            listOf(MealType.DINNER),
            CustomDietPlanScheduleResolver.mealTypesFor(
                mealOrder = emptyList(),
                mealCount = 1
            )
        )
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.DINNER),
            CustomDietPlanScheduleResolver.mealTypesFor(
                mealOrder = emptyList(),
                mealCount = 2
            )
        )
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
            CustomDietPlanScheduleResolver.mealTypesFor(
                mealOrder = emptyList(),
                mealCount = 3
            )
        )
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.SNACK, MealType.LUNCH, MealType.DINNER),
            CustomDietPlanScheduleResolver.mealTypesFor(
                mealOrder = emptyList(),
                mealCount = 9
            )
        )
    }

    @Test
    fun mealRatios_normalizesSelectedMealRatios() {
        val ratios = CustomDietPlanScheduleResolver.mealRatios(
            listOf(MealType.BREAKFAST, MealType.LUNCH)
        )

        assertEquals(0.416, ratios.getValue(MealType.BREAKFAST), 0.001)
        assertEquals(0.583, ratios.getValue(MealType.LUNCH), 0.001)
    }

    @Test
    fun calories_areRoundedAndClampedLikeGeneratedMeals() {
        val targetCalories = CustomDietPlanScheduleResolver.targetMealCalories(
            dailyCalories = 1800,
            ratio = 0.25
        )
        val baseCalories = CustomDietPlanScheduleResolver.baseCalories(
            targetMealCalories = targetCalories,
            dailyCalories = 1800
        )

        assertEquals(450, targetCalories)
        assertEquals(450, baseCalories)
        assertEquals(
            60,
            CustomDietPlanScheduleResolver.targetMealCalories(
                dailyCalories = 100,
                ratio = 0.10
            )
        )
    }
}
