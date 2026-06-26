package com.example.fitto.feature_recipes

import kotlin.math.roundToInt

object RecipePortionNutritionResolver {

    data class State(
        val baseServings: Int,
        val activeServings: Int,
        val perServingCalories: Int,
        val activeTotalCalories: Int,
        val macros: MacroSummary,
        val macroPercent: MacroPercentSummary
    )

    data class MacroSummary(
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )

    data class MacroPercentSummary(
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )

    fun resolve(
        recipeCalories: Int,
        recipeServings: Int,
        servingMultiplier: Int,
        ingredientCalories: List<Int>,
        proteinGrams: Int,
        carbGrams: Int,
        fatGrams: Int
    ): State {
        val baseServings = recipeServings.coerceAtLeast(1)
        val activeServings = baseServings * servingMultiplier.coerceAtLeast(1)
        val perServingCalories = resolveCaloriesPerServing(
            recipeCalories = recipeCalories,
            ingredientCalories = ingredientCalories,
            baseServings = baseServings
        )
        val activeTotalCalories = perServingCalories * activeServings
        val macros = resolveMacros(
            activeTotalCalories = activeTotalCalories,
            activeServings = activeServings,
            baseServings = baseServings,
            proteinGrams = proteinGrams,
            carbGrams = carbGrams,
            fatGrams = fatGrams
        )

        return State(
            baseServings = baseServings,
            activeServings = activeServings,
            perServingCalories = perServingCalories,
            activeTotalCalories = activeTotalCalories,
            macros = macros,
            macroPercent = resolveMacroPercent(macros)
        )
    }

    private fun resolveCaloriesPerServing(
        recipeCalories: Int,
        ingredientCalories: List<Int>,
        baseServings: Int
    ): Int {
        val estimatedTotal = ingredientCalories.sum()
        val estimatedPerServing = (estimatedTotal / baseServings.toDouble()).roundToInt()

        return maxOf(recipeCalories, estimatedPerServing, 0)
    }

    private fun resolveMacros(
        activeTotalCalories: Int,
        activeServings: Int,
        baseServings: Int,
        proteinGrams: Int,
        carbGrams: Int,
        fatGrams: Int
    ): MacroSummary {
        val hasSourceMacros = proteinGrams > 0 || carbGrams > 0 || fatGrams > 0

        if (hasSourceMacros) {
            val scale = activeServings.toDouble() / baseServings.toDouble()

            return MacroSummary(
                protein = (proteinGrams * scale).roundToInt().coerceAtLeast(0),
                carbs = (carbGrams * scale).roundToInt().coerceAtLeast(0),
                fat = (fatGrams * scale).roundToInt().coerceAtLeast(0)
            )
        }

        return MacroSummary(
            protein = (activeTotalCalories * 0.15 / 4).roundToInt().coerceAtLeast(0),
            carbs = (activeTotalCalories * 0.55 / 4).roundToInt().coerceAtLeast(0),
            fat = (activeTotalCalories * 0.30 / 9).roundToInt().coerceAtLeast(0)
        )
    }

    private fun resolveMacroPercent(
        macros: MacroSummary
    ): MacroPercentSummary {
        val proteinCalories = macros.protein * 4
        val carbCalories = macros.carbs * 4
        val fatCalories = macros.fat * 9
        val total = (proteinCalories + carbCalories + fatCalories).coerceAtLeast(1)

        return MacroPercentSummary(
            protein = ((proteinCalories * 100.0) / total).roundToInt().coerceIn(0, 100),
            carbs = ((carbCalories * 100.0) / total).roundToInt().coerceIn(0, 100),
            fat = ((fatCalories * 100.0) / total).roundToInt().coerceIn(0, 100)
        )
    }
}
