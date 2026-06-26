package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanStateStoreTest {

    @Test
    fun initialStateUsesExpectedDefaults() {
        val store = CreateDietPlanStateStore()

        val renderState = store.toRenderState()

        assertEquals(1800, renderState.dailyCalories)
        assertEquals(30, renderState.durationDays)
        assertEquals(CreatePlanGoal.WEIGHT_LOSS, renderState.selectedGoal)
        assertEquals(CreatePlanPrivacy.PRIVATE, renderState.selectedPrivacy)
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.SNACK, MealType.LUNCH, MealType.DINNER),
            renderState.selectedMealTypes
        )
        assertEquals(emptySet<String>(), renderState.selectedPreferences)
    }

    @Test
    fun applyDefaultsUpdatesProfileDrivenValuesOnly() {
        val store = CreateDietPlanStateStore()
        store.setPrivacy(CreatePlanPrivacy.PUBLIC)
        store.setDurationDays(45)

        store.applyDefaults(
            CreateDietPlanDefaults(
                dailyCalories = 2200,
                goal = CreatePlanGoal.MUSCLE
            )
        )

        val renderState = store.toRenderState()
        assertEquals(2200, renderState.dailyCalories)
        assertEquals(CreatePlanGoal.MUSCLE, renderState.selectedGoal)
        assertEquals(CreatePlanPrivacy.PUBLIC, renderState.selectedPrivacy)
        assertEquals(45, renderState.durationDays)
    }

    @Test
    fun applyEditableStateReplacesSelections() {
        val store = CreateDietPlanStateStore(
            initialPreferences = listOf(CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN),
            initialMealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH)
        )

        store.applyEditableState(
            CreateDietPlanEditableState(
                planName = "Draft",
                description = "Description",
                goal = CreatePlanGoal.BALANCED,
                privacy = CreatePlanPrivacy.PUBLIC,
                dailyCalories = 1950,
                durationDays = 21,
                mealTypes = listOf(MealType.DINNER),
                preferences = listOf(CustomDietMealPlanGenerator.PREF_LOW_CARB),
                exclusions = listOf("Mushroom")
            )
        )

        val formState = store.toFormState(
            planName = "Plan",
            description = "Description",
            exclusions = listOf("Mushroom")
        )

        assertEquals(CreatePlanGoal.BALANCED, formState.goal)
        assertEquals(CreatePlanPrivacy.PUBLIC, formState.privacy)
        assertEquals(1950, formState.dailyCalories)
        assertEquals(21, formState.durationDays)
        assertEquals(listOf(MealType.DINNER), formState.mealTypes)
        assertEquals(listOf(CustomDietMealPlanGenerator.PREF_LOW_CARB), formState.preferences)
        assertEquals(listOf("Mushroom"), formState.exclusions)
    }

    @Test
    fun togglePreferenceUsesSelectionRules() {
        val store = CreateDietPlanStateStore(
            initialPreferences = listOf(CustomDietMealPlanGenerator.PREF_VEGETARIAN)
        )

        store.togglePreference(CustomDietMealPlanGenerator.PREF_VEGAN, isChecked = true)

        assertEquals(
            setOf(CustomDietMealPlanGenerator.PREF_VEGAN),
            store.toRenderState().selectedPreferences
        )
    }

    @Test
    fun toggleMealTypeKeepsLastSelectedMeal() {
        val store = CreateDietPlanStateStore(
            initialMealTypes = listOf(MealType.BREAKFAST)
        )

        store.toggleMealType(MealType.BREAKFAST, isChecked = false)

        assertEquals(
            listOf(MealType.BREAKFAST),
            store.toRenderState().selectedMealTypes
        )
    }
}
