package com.example.fitto.feature_daily

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyCustomPlanLoadActionTest {

    @Test
    fun `loads foods progress and generated custom plan`() = runBlocking {
        val customPlan = dailyTestCustomPlan(durationDays = 21)
        val activePlan = dailyTestActivePlan(id = customPlan.id)
        val foods = listOf(dailyTestFood())
        val dailyPlan = dailyTestMealPlan(planId = customPlan.id)
        var requestedDuration = 0
        var completedRequest: Pair<String, Int>? = null
        var generatorInput: List<Any>? = null
        val action = DailyCustomPlanLoadAction(
            getAllFoods = { foods },
            getCurrentPlanDay = { durationDays ->
                requestedDuration = durationDays
                4
            },
            getCompletedMealIds = { planId, dayNumber ->
                completedRequest = planId to dayNumber
                setOf("breakfast")
            },
            createActivePlan = {
                assertEquals(customPlan, it)
                activePlan
            },
            buildDailyPlan = { plan, loadedFoods, dayNumber, completedIds ->
                generatorInput = listOf(plan, loadedFoods, dayNumber, completedIds)
                dailyPlan
            }
        )

        val result = action.load(customPlan)

        assertEquals(21, requestedDuration)
        assertEquals(customPlan.id to 4, completedRequest)
        assertEquals(
            listOf(customPlan, foods, 4, setOf("breakfast")),
            generatorInput
        )
        assertEquals(activePlan, result.activePlan)
        assertEquals(dailyPlan, result.dailyPlan)
    }
}
