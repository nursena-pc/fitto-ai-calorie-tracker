package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyCatalogPlanLoadActionTest {

    @Test
    fun `unknown catalog plan returns null`() {
        val action = action(durationDays = 0)

        assertNull(action.load(dailyTestActivePlan(), 1800))
    }

    @Test
    fun `empty catalog day returns null`() {
        val action = action(
            durationDays = 7,
            meals = emptyList()
        )

        assertNull(action.load(dailyTestActivePlan(), 1800))
    }

    @Test
    fun `builds catalog plan and resolves next incomplete meal`() {
        val activePlan = dailyTestActivePlan()
        val breakfast = dailyTestPlannedMeal(id = "breakfast")
        val lunch = dailyTestPlannedMeal(id = "lunch", mealType = MealType.LUNCH)
        val action = action(
            durationDays = 14,
            dayNumber = 5,
            meals = listOf(breakfast, lunch),
            completedMealIds = setOf("breakfast")
        )

        val result = action.load(activePlan, 2000)

        assertEquals("Catalog title", result?.planTitle)
        assertEquals("Catalog badge", result?.planBadge)
        assertEquals("catalog-icon", result?.planIcon)
        assertEquals(5, result?.dayNumber)
        assertEquals(14, result?.durationDays)
        assertEquals(2000, result?.targetCalories)
        assertEquals(lunch, result?.nextMeal)
    }

    private fun action(
        durationDays: Int,
        dayNumber: Int = 1,
        meals: List<com.example.fitto.feature_dietplans.planning.PlannedMealUiModel> = listOf(
            dailyTestPlannedMeal()
        ),
        completedMealIds: Set<String> = emptySet()
    ): DailyCatalogPlanLoadAction {
        return DailyCatalogPlanLoadAction(
            getPlanDurationDays = { durationDays },
            getCurrentPlanDay = {
                assertEquals(durationDays, it)
                dayNumber
            },
            getMealsForDay = { planId, requestedDay ->
                assertEquals("balanced", planId)
                assertEquals(dayNumber, requestedDay)
                meals
            },
            getCompletedMealIds = { _, _ -> completedMealIds },
            getPlanTitle = { "Catalog title" },
            getPlanBadge = { "Catalog badge" },
            getPlanIcon = { "catalog-icon" }
        )
    }
}
