package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailySeedPlanLoadActionTest {

    @Test
    fun `missing room plan returns null without loading meals`() = runBlocking {
        var mealLoadCount = 0
        val action = DailySeedPlanLoadAction(
            getDietPlanById = { null },
            getPlannedMealsForDay = { _, _ ->
                mealLoadCount += 1
                emptyList()
            },
            getCurrentPlanDay = { 1 },
            getCompletedMealIds = { _, _ -> emptySet() }
        )

        assertNull(action.load(dailyTestActivePlan(), 1800))
        assertEquals(0, mealLoadCount)
    }

    @Test
    fun `invalid room duration returns null`() = runBlocking {
        val action = DailySeedPlanLoadAction(
            getDietPlanById = { dailyTestDietPlanEntity(durationDays = 0) },
            getPlannedMealsForDay = { _, _ -> error("Meals must not be loaded") },
            getCurrentPlanDay = { error("Day must not be resolved") },
            getCompletedMealIds = { _, _ -> error("Progress must not be loaded") }
        )

        assertNull(action.load(dailyTestActivePlan(), 1800))
    }

    @Test
    fun `builds room plan and resolves next incomplete meal`() = runBlocking {
        val activePlan = dailyTestActivePlan(icon = "catalog-icon")
        val breakfast = dailyTestPlannedMeal(id = "breakfast")
        val lunch = dailyTestPlannedMeal(
            id = "lunch",
            mealType = MealType.LUNCH
        )
        val action = DailySeedPlanLoadAction(
            getDietPlanById = {
                assertEquals(activePlan.id, it)
                dailyTestDietPlanEntity(
                    id = activePlan.id,
                    nameTr = "Room title",
                    icon = ""
                )
            },
            getPlannedMealsForDay = { planId, dayNumber ->
                assertEquals(activePlan.id, planId)
                assertEquals(3, dayNumber)
                listOf(breakfast, lunch)
            },
            getCurrentPlanDay = { durationDays ->
                assertEquals(7, durationDays)
                3
            },
            getCompletedMealIds = { _, _ -> setOf("breakfast") }
        )

        val result = action.load(activePlan, 2100)

        assertEquals("Room title", result?.planTitle)
        assertEquals("catalog-icon", result?.planIcon)
        assertEquals(2100, result?.targetCalories)
        assertEquals(lunch, result?.nextMeal)
        assertEquals(setOf("breakfast"), result?.completedMealIds)
    }
}
