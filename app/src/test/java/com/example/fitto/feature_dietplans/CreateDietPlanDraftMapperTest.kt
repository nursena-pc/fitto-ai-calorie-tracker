package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanDraftMapperTest {

    @Test
    fun toEditableState_mapsDraftFields() {
        val state = CreateDietPlanDraftMapper.toEditableState(
            draft(
                goalType = CreatePlanGoal.MUSCLE.name,
                isPublic = true,
                mealOrder = listOf("dinner", "breakfast")
            )
        )

        assertEquals("Plan title", state.planName)
        assertEquals("Plan description", state.description)
        assertEquals(CreatePlanGoal.MUSCLE, state.goal)
        assertEquals(CreatePlanPrivacy.PUBLIC, state.privacy)
        assertEquals(2200, state.dailyCalories)
        assertEquals(21, state.durationDays)
        assertEquals(listOf(MealType.DINNER, MealType.BREAKFAST), state.mealTypes)
        assertEquals(listOf("high_protein"), state.preferences)
        assertEquals(listOf("milk"), state.exclusions)
    }

    @Test
    fun toEditableState_usesBalancedGoalWhenDraftGoalIsUnknown() {
        val state = CreateDietPlanDraftMapper.toEditableState(
            draft(goalType = "UNKNOWN")
        )

        assertEquals(CreatePlanGoal.BALANCED, state.goal)
    }

    @Test
    fun toEditableState_mapsPrivatePrivacy() {
        val state = CreateDietPlanDraftMapper.toEditableState(
            draft(isPublic = false)
        )

        assertEquals(CreatePlanPrivacy.PRIVATE, state.privacy)
    }

    @Test
    fun toEditableState_skipsUnknownMealsAndPreservesKnownMealOrder() {
        val state = CreateDietPlanDraftMapper.toEditableState(
            draft(mealOrder = listOf("unknown", "snack", "lunch"))
        )

        assertEquals(listOf(MealType.SNACK, MealType.LUNCH), state.mealTypes)
    }

    @Test
    fun toEditableState_usesDefaultMealOrderWhenDraftMealsAreEmptyOrUnknown() {
        val emptyMealsState = CreateDietPlanDraftMapper.toEditableState(
            draft(mealOrder = emptyList())
        )
        val unknownMealsState = CreateDietPlanDraftMapper.toEditableState(
            draft(mealOrder = listOf("unknown"))
        )
        val defaultMeals = listOf(
            MealType.BREAKFAST,
            MealType.SNACK,
            MealType.LUNCH,
            MealType.DINNER
        )

        assertEquals(defaultMeals, emptyMealsState.mealTypes)
        assertEquals(defaultMeals, unknownMealsState.mealTypes)
    }

    private fun draft(
        goalType: String = CreatePlanGoal.WEIGHT_LOSS.name,
        isPublic: Boolean = false,
        mealOrder: List<String> = listOf("breakfast", "snack")
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom_plan_1",
            title = "Plan title",
            description = "Plan description",
            goalType = goalType,
            dailyCalories = 2200,
            mealCount = mealOrder.size,
            durationDays = 21,
            isPublic = isPublic,
            preferences = listOf("high_protein"),
            exclusions = listOf("milk"),
            mealOrder = mealOrder,
            isDraft = true,
            createdAt = 100L,
            updatedAt = 200L
        )
    }
}
