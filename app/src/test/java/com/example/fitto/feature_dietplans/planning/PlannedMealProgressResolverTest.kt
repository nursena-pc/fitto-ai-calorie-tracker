package com.example.fitto.feature_dietplans.planning

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannedMealProgressResolverTest {

    @Test
    fun nextMeal_returnsFirstMealNotCompleted() {
        val meals = listOf(
            plannedMeal("breakfast", MealType.BREAKFAST),
            plannedMeal("lunch", MealType.LUNCH),
            plannedMeal("dinner", MealType.DINNER)
        )

        assertEquals(
            meals[1],
            PlannedMealProgressResolver.nextMeal(
                meals = meals,
                completedMealIds = setOf("breakfast")
            )
        )
    }

    @Test
    fun nextMeal_returnsNullWhenAllMealsCompleted() {
        val meals = listOf(
            plannedMeal("breakfast", MealType.BREAKFAST),
            plannedMeal("lunch", MealType.LUNCH)
        )

        assertNull(
            PlannedMealProgressResolver.nextMeal(
                meals = meals,
                completedMealIds = setOf("breakfast", "lunch")
            )
        )
    }

    @Test
    fun progressPercent_matchesCompletedIdRatio() {
        val meals = listOf(
            plannedMeal("breakfast", MealType.BREAKFAST),
            plannedMeal("lunch", MealType.LUNCH),
            plannedMeal("dinner", MealType.DINNER)
        )

        assertEquals(
            67,
            PlannedMealProgressResolver.progressPercent(
                meals = meals,
                completedMealIds = setOf("breakfast", "lunch")
            )
        )
    }

    @Test
    fun progressPercent_returnsZeroForEmptyMealList() {
        assertEquals(
            0,
            PlannedMealProgressResolver.progressPercent(
                meals = emptyList(),
                completedMealIds = setOf("breakfast")
            )
        )
    }

    @Test
    fun isCompleted_requiresAtLeastOneMealAndEnoughCompletedIds() {
        assertFalse(
            PlannedMealProgressResolver.isCompleted(
                meals = emptyList(),
                completedMealIds = emptySet()
            )
        )
        assertTrue(
            PlannedMealProgressResolver.isCompleted(
                meals = listOf(plannedMeal("breakfast", MealType.BREAKFAST)),
                completedMealIds = setOf("breakfast")
            )
        )
    }

    private fun plannedMeal(
        id: String,
        mealType: MealType
    ) = PlannedMealUiModel(
        id = id,
        planId = "plan",
        dayNumber = 1,
        mealType = mealType,
        title = id,
        baseCalories = 400,
        foods = listOf("Food")
    )
}
