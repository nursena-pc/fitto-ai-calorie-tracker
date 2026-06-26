package com.example.fitto.feature_dietplans

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDietPlanPreviewResolverTest {

    @Test
    fun buildPreviewMeals_usesDefaultMealRatiosWhenAllMealsAreSelected() {
        val meals = CreateDietPlanPreviewResolver.buildPreviewMeals(
            selectedMealTypes = listOf(
                MealType.BREAKFAST,
                MealType.SNACK,
                MealType.LUNCH,
                MealType.DINNER
            ),
            dailyCalories = 1800
        )

        assertEquals(25, meals.getValue(MealType.BREAKFAST).percent)
        assertEquals(450, meals.getValue(MealType.BREAKFAST).calories)
        assertEquals(15, meals.getValue(MealType.SNACK).percent)
        assertEquals(270, meals.getValue(MealType.SNACK).calories)
        assertEquals(35, meals.getValue(MealType.LUNCH).percent)
        assertEquals(630, meals.getValue(MealType.LUNCH).calories)
        assertEquals(25, meals.getValue(MealType.DINNER).percent)
        assertEquals(450, meals.getValue(MealType.DINNER).calories)
    }

    @Test
    fun buildPreviewMeals_normalizesRatiosForSelectedMealsOnly() {
        val meals = CreateDietPlanPreviewResolver.buildPreviewMeals(
            selectedMealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH),
            dailyCalories = 1800
        )

        assertEquals(41, meals.getValue(MealType.BREAKFAST).percent)
        assertEquals(750, meals.getValue(MealType.BREAKFAST).calories)
        assertEquals(58, meals.getValue(MealType.LUNCH).percent)
        assertEquals(1050, meals.getValue(MealType.LUNCH).calories)
    }

    @Test
    fun buildPreviewMeals_returnsEmptyMapWhenNoMealIsSelected() {
        val meals = CreateDietPlanPreviewResolver.buildPreviewMeals(
            selectedMealTypes = emptyList(),
            dailyCalories = 1800
        )

        assertTrue(meals.isEmpty())
    }

    @Test
    fun mealIcon_returnsExistingPreviewIcons() {
        assertEquals("☀️", CreateDietPlanPreviewResolver.mealIcon(MealType.BREAKFAST))
        assertEquals("🥜", CreateDietPlanPreviewResolver.mealIcon(MealType.SNACK))
        assertEquals("🍽️", CreateDietPlanPreviewResolver.mealIcon(MealType.LUNCH))
        assertEquals("🌙", CreateDietPlanPreviewResolver.mealIcon(MealType.DINNER))
    }
}
