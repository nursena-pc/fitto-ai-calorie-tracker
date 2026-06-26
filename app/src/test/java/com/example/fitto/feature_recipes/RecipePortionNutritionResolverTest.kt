package com.example.fitto.feature_recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipePortionNutritionResolverTest {

    @Test
    fun resolve_prefersRecipeCaloriesOverIngredientEstimate() {
        val state = RecipePortionNutritionResolver.resolve(
            recipeCalories = 300,
            recipeServings = 2,
            servingMultiplier = 2,
            ingredientCalories = listOf(100, 100),
            proteinGrams = 20,
            carbGrams = 30,
            fatGrams = 10
        )

        assertEquals(2, state.baseServings)
        assertEquals(4, state.activeServings)
        assertEquals(300, state.perServingCalories)
        assertEquals(1200, state.activeTotalCalories)
        assertEquals(
            RecipePortionNutritionResolver.MacroSummary(
                protein = 40,
                carbs = 60,
                fat = 20
            ),
            state.macros
        )
        assertEquals(
            RecipePortionNutritionResolver.MacroPercentSummary(
                protein = 28,
                carbs = 41,
                fat = 31
            ),
            state.macroPercent
        )
    }

    @Test
    fun resolve_usesIngredientCaloriesWhenRecipeCaloriesAreLower() {
        val state = RecipePortionNutritionResolver.resolve(
            recipeCalories = 0,
            recipeServings = 2,
            servingMultiplier = 1,
            ingredientCalories = listOf(150, 250),
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0
        )

        assertEquals(200, state.perServingCalories)
        assertEquals(400, state.activeTotalCalories)
        assertEquals(
            RecipePortionNutritionResolver.MacroSummary(
                protein = 15,
                carbs = 55,
                fat = 13
            ),
            state.macros
        )
        assertEquals(
            RecipePortionNutritionResolver.MacroPercentSummary(
                protein = 15,
                carbs = 55,
                fat = 29
            ),
            state.macroPercent
        )
    }

    @Test
    fun resolve_usesSafeServingMinimums() {
        val state = RecipePortionNutritionResolver.resolve(
            recipeCalories = 100,
            recipeServings = 0,
            servingMultiplier = 0,
            ingredientCalories = emptyList(),
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0
        )

        assertEquals(1, state.baseServings)
        assertEquals(1, state.activeServings)
        assertEquals(100, state.perServingCalories)
    }
}
