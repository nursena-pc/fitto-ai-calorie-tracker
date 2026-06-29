package com.example.fitto.feature_recipes.data

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_recipes.util.normalizeRecipeText
import java.util.Locale

object RecipeFilterResolver {

    fun ingredientOptions(recipes: List<RecipeUiModel>): List<RecipeFilterOption> {
        return recipes
            .flatMap { recipe -> recipe.ingredientNames }
            .mapNotNull { ingredientName -> ingredientName.toIngredientOptionOrNull() }
            .distinctBy { option -> option.key }
            .sortedBy { option -> option.label.normalizeRecipeText() }
    }

    fun filterRecipes(
        recipes: List<RecipeUiModel>,
        query: String,
        filterState: RecipeFilterState
    ): List<RecipeUiModel> {
        val normalizedQuery = query.normalizeRecipeText()

        return recipes.filter { recipe ->
            recipe.matchesQuery(normalizedQuery) &&
                recipe.matchesMainCategory(filterState) &&
                recipe.matchesSubcategory(filterState) &&
                recipe.matchesMainIngredient(filterState) &&
                recipe.matchesDiets(filterState) &&
                recipe.matchesMethods(filterState) &&
                recipe.matchesDuration(filterState) &&
                recipe.matchesDifficulty(filterState) &&
                recipe.matchesIncludedIngredients(filterState) &&
                recipe.matchesExcludedIngredients(filterState)
        }
    }

    private fun RecipeUiModel.matchesQuery(normalizedQuery: String): Boolean {
        return normalizedQuery.isBlank() || searchableText().contains(normalizedQuery)
    }

    private fun RecipeUiModel.matchesMainCategory(filterState: RecipeFilterState): Boolean {
        return filterState.selectedMainCategoryKey
            ?.let { selectedCategory -> mainCategoryKey == selectedCategory }
            ?: true
    }

    private fun RecipeUiModel.matchesSubcategory(filterState: RecipeFilterState): Boolean {
        return filterState.selectedSubcategoryKey
            ?.let { selectedSubcategory -> selectedSubcategory in subcategoryKeys }
            ?: true
    }

    private fun RecipeUiModel.matchesMainIngredient(filterState: RecipeFilterState): Boolean {
        return filterState.selectedMainIngredientKeys.isEmpty() ||
            mainIngredientKeys.any { ingredientKey -> ingredientKey in filterState.selectedMainIngredientKeys }
    }

    private fun RecipeUiModel.matchesDiets(filterState: RecipeFilterState): Boolean {
        return filterState.selectedDietKeys.isEmpty() ||
            filterState.selectedDietKeys.all { dietKey -> dietKey in dietKeys }
    }

    private fun RecipeUiModel.matchesMethods(filterState: RecipeFilterState): Boolean {
        return filterState.selectedMethodKeys.isEmpty() ||
            methodKeys.any { methodKey -> methodKey in filterState.selectedMethodKeys }
    }

    private fun RecipeUiModel.matchesDuration(filterState: RecipeFilterState): Boolean {
        return filterState.selectedDurationKey
            ?.let { selectedDuration -> durationLabel == selectedDuration }
            ?: true
    }

    private fun RecipeUiModel.matchesDifficulty(filterState: RecipeFilterState): Boolean {
        return filterState.selectedDifficultyKey
            ?.let { selectedDifficulty -> difficultyKey == selectedDifficulty }
            ?: true
    }

    private fun RecipeUiModel.matchesIncludedIngredients(filterState: RecipeFilterState): Boolean {
        if (filterState.selectedIncludedIngredientKeys.isEmpty()) return true
        val ingredientSearchPool = normalizedIngredientSearchPool()
        return filterState.selectedIncludedIngredientKeys.all { selectedIngredientKey ->
            ingredientSearchPool.any { ingredient -> ingredient.contains(selectedIngredientKey) }
        }
    }

    private fun RecipeUiModel.matchesExcludedIngredients(filterState: RecipeFilterState): Boolean {
        if (filterState.selectedExcludedIngredientKeys.isEmpty()) return true
        val ingredientSearchPool = normalizedIngredientSearchPool()
        return filterState.selectedExcludedIngredientKeys.none { selectedIngredientKey ->
            ingredientSearchPool.any { ingredient -> ingredient.contains(selectedIngredientKey) }
        }
    }

    private fun RecipeUiModel.normalizedIngredientSearchPool(): List<String> {
        return ingredientNames.map { ingredientName -> ingredientName.normalizeRecipeText() }
    }

    private fun RecipeUiModel.searchableText(): String {
        return buildString {
            append(title).append(' ')
            append(description).append(' ')
            append(mainCategoryKey).append(' ')
            append(subcategoryKeys.joinToString(" ")).append(' ')
            append(mainIngredientKeys.joinToString(" ")).append(' ')
            append(dietKeys.joinToString(" ")).append(' ')
            append(methodKeys.joinToString(" ")).append(' ')
            append(ingredientNames.joinToString(" ")).append(' ')
            append(ingredients.joinToString(" ")).append(' ')
            append(steps.joinToString(" ")).append(' ')
            append(durationLabel).append(' ')
            append(durationMinutes).append(" dk ")
            append(servingDisplay).append(' ')
            append(difficultyKey).append(' ')
            append(tip.orEmpty())
        }.normalizeRecipeText()
    }

    private fun String.toIngredientOptionOrNull(): RecipeFilterOption? {
        val label = substringBefore(" - ")
            .substringBefore("(")
            .trim()
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
            }
        if (label.isBlank()) return null
        return RecipeFilterOption(key = label.normalizeRecipeText(), label = label)
    }

    private val TURKISH_LOCALE = Locale("tr", "TR")
}
