package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanSelectionReducerTest {

    @Test
    fun togglePreference_selectingVeganRemovesVegetarian() {
        val preferences = CreateDietPlanSelectionReducer.togglePreference(
            currentPreferences = setOf(
                CustomDietMealPlanGenerator.PREF_VEGETARIAN,
                CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN
            ),
            preferenceKey = CustomDietMealPlanGenerator.PREF_VEGAN,
            isChecked = true
        )

        assertEquals(
            setOf(
                CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN,
                CustomDietMealPlanGenerator.PREF_VEGAN
            ),
            preferences
        )
    }

    @Test
    fun togglePreference_selectingVegetarianRemovesVegan() {
        val preferences = CreateDietPlanSelectionReducer.togglePreference(
            currentPreferences = setOf(
                CustomDietMealPlanGenerator.PREF_VEGAN,
                CustomDietMealPlanGenerator.PREF_LOW_CARB
            ),
            preferenceKey = CustomDietMealPlanGenerator.PREF_VEGETARIAN,
            isChecked = true
        )

        assertEquals(
            setOf(
                CustomDietMealPlanGenerator.PREF_LOW_CARB,
                CustomDietMealPlanGenerator.PREF_VEGETARIAN
            ),
            preferences
        )
    }

    @Test
    fun togglePreference_uncheckingRemovesPreference() {
        val preferences = CreateDietPlanSelectionReducer.togglePreference(
            currentPreferences = setOf(
                CustomDietMealPlanGenerator.PREF_LOW_CARB,
                CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN
            ),
            preferenceKey = CustomDietMealPlanGenerator.PREF_LOW_CARB,
            isChecked = false
        )

        assertEquals(
            setOf(CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN),
            preferences
        )
    }

    @Test
    fun toggleMealType_addsAndRemovesMealsWhileKeepingOrder() {
        val added = CreateDietPlanSelectionReducer.toggleMealType(
            currentMealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH),
            mealType = MealType.DINNER,
            isChecked = true
        )
        val removed = CreateDietPlanSelectionReducer.toggleMealType(
            currentMealTypes = added,
            mealType = MealType.BREAKFAST,
            isChecked = false
        )

        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
            added.toList()
        )
        assertEquals(
            listOf(MealType.LUNCH, MealType.DINNER),
            removed.toList()
        )
    }

    @Test
    fun toggleMealType_keepsLastSelectedMeal() {
        val meals = CreateDietPlanSelectionReducer.toggleMealType(
            currentMealTypes = listOf(MealType.BREAKFAST),
            mealType = MealType.BREAKFAST,
            isChecked = false
        )

        assertEquals(listOf(MealType.BREAKFAST), meals.toList())
    }
}
