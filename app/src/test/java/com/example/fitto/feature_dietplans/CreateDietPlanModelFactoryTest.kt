package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDietPlanModelFactoryTest {

    @Test
    fun create_buildsNewPlanFromFormState() {
        val plan = CreateDietPlanModelFactory.create(
            formState = formState(),
            existingPlan = null,
            isDraft = false,
            now = NOW
        )

        assertEquals("${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$NOW", plan.id)
        assertEquals("My plan", plan.title)
        assertEquals("Description", plan.description)
        assertEquals(CreatePlanGoal.MUSCLE.name, plan.goalType)
        assertEquals(2100, plan.dailyCalories)
        assertEquals(2, plan.mealCount)
        assertEquals(30, plan.durationDays)
        assertTrue(plan.isPublic)
        assertEquals(listOf("high_protein"), plan.preferences)
        assertEquals(listOf("milk"), plan.exclusions)
        assertEquals(listOf("breakfast", "dinner"), plan.mealOrder)
        assertFalse(plan.isDraft)
        assertEquals(NOW, plan.createdAt)
        assertEquals(NOW, plan.updatedAt)
    }

    @Test
    fun create_preservesExistingPlanIdentityAndCreatedAtWhenUpdatingDraft() {
        val plan = CreateDietPlanModelFactory.create(
            formState = formState(privacy = CreatePlanPrivacy.PRIVATE),
            existingPlan = existingPlan(),
            isDraft = true,
            now = NOW
        )

        assertEquals("custom_plan_existing", plan.id)
        assertEquals(500L, plan.createdAt)
        assertEquals(NOW, plan.updatedAt)
        assertTrue(plan.isDraft)
        assertFalse(plan.isPublic)
    }

    private fun formState(
        privacy: CreatePlanPrivacy = CreatePlanPrivacy.PUBLIC
    ): CreateDietPlanFormState {
        return CreateDietPlanFormState(
            planName = "My plan",
            description = "Description",
            goal = CreatePlanGoal.MUSCLE,
            privacy = privacy,
            dailyCalories = 2100,
            durationDays = 30,
            mealTypes = listOf(MealType.BREAKFAST, MealType.DINNER),
            preferences = listOf("high_protein"),
            exclusions = listOf("milk")
        )
    }

    private fun existingPlan(): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom_plan_existing",
            title = "Old title",
            description = "Old description",
            goalType = CreatePlanGoal.BALANCED.name,
            dailyCalories = 1800,
            mealCount = 4,
            durationDays = 14,
            isPublic = true,
            preferences = emptyList(),
            exclusions = emptyList(),
            mealOrder = emptyList(),
            isDraft = true,
            createdAt = 500L,
            updatedAt = 500L
        )
    }

    private companion object {
        const val NOW = 1_000L
    }
}
