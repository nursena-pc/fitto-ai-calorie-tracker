package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_meal.model.MealType

object CreateDietPlanSelectionReducer {

    fun togglePreference(
        currentPreferences: Collection<String>,
        preferenceKey: String,
        isChecked: Boolean
    ): Set<String> {
        return currentPreferences.toMutableSet().apply {
            if (isChecked) {
                when (preferenceKey) {
                    CustomDietMealPlanGenerator.PREF_VEGAN ->
                        remove(CustomDietMealPlanGenerator.PREF_VEGETARIAN)

                    CustomDietMealPlanGenerator.PREF_VEGETARIAN ->
                        remove(CustomDietMealPlanGenerator.PREF_VEGAN)
                }

                add(preferenceKey)
            } else {
                remove(preferenceKey)
            }
        }
    }

    fun toggleMealType(
        currentMealTypes: Collection<MealType>,
        mealType: MealType,
        isChecked: Boolean
    ): Set<MealType> {
        val updatedMealTypes = currentMealTypes.toMutableSet()

        if (isChecked) {
            updatedMealTypes.add(mealType)
            return updatedMealTypes
        }

        if (updatedMealTypes.size <= MIN_SELECTED_MEAL_COUNT) {
            return currentMealTypes.toSet()
        }

        updatedMealTypes.remove(mealType)
        return updatedMealTypes
    }

    private const val MIN_SELECTED_MEAL_COUNT = 1
}
