package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomDietPlanDetailMapperTest {

    @Test
    fun mapUsesPlanValuesAndDerivedTargets() {
        val plan = customPlan(
            goalType = "MUSCLE",
            dailyCalories = 2000,
            preferences = listOf("high_protein", "low_carb"),
            exclusions = listOf("Mushroom"),
            mealOrder = listOf("breakfast", "dinner")
        )

        val detail = CustomDietPlanDetailMapper.map(plan)

        assertEquals("My Plan", detail.title)
        assertEquals("Description", detail.description)
        assertEquals(CreatePlanGoal.MUSCLE, detail.goal)
        assertEquals(2000, detail.dailyCalories)
        assertEquals(3, detail.mealCount)
        assertEquals(14, detail.durationDays)
        assertEquals(225, detail.macroTargets.carbGrams)
        assertEquals(125, detail.macroTargets.proteinGrams)
        assertEquals(67, detail.macroTargets.fatGrams)
        assertEquals(listOf("high_protein", "low_carb"), detail.preferences)
        assertEquals(listOf("Mushroom"), detail.exclusions)
        assertEquals(listOf("breakfast", "dinner"), detail.mealOrder)
    }

    @Test
    fun goalFromFallsBackToBalancedForUnknownGoal() {
        assertEquals(
            CreatePlanGoal.BALANCED,
            CustomDietPlanDetailMapper.goalFrom("unexpected")
        )
    }

    @Test
    fun goalFromAcceptsLowercaseStoredEnumName() {
        assertEquals(
            CreatePlanGoal.WEIGHT_LOSS,
            CustomDietPlanDetailMapper.goalFrom("weight_loss")
        )
    }

    @Test
    fun macroTargetsUseExpectedDistribution() {
        val targets = CustomDietPlanDetailMapper.macroTargetsFor(1800)

        assertEquals(203, targets.carbGrams)
        assertEquals(113, targets.proteinGrams)
        assertEquals(60, targets.fatGrams)
    }

    private fun customPlan(
        goalType: String = "BALANCED",
        dailyCalories: Int = 1800,
        preferences: List<String> = emptyList(),
        exclusions: List<String> = emptyList(),
        mealOrder: List<String> = emptyList()
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom-1",
            title = "My Plan",
            description = "Description",
            goalType = goalType,
            dailyCalories = dailyCalories,
            mealCount = 3,
            durationDays = 14,
            isPublic = false,
            preferences = preferences,
            exclusions = exclusions,
            mealOrder = mealOrder,
            isDraft = false,
            createdAt = 1L,
            updatedAt = 2L
        )
    }
}
