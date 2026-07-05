package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.util.normalizeRecipeText

enum class RecipeIngredientFilterMode {
    INCLUDE,
    EXCLUDE
}

data class RecipeIngredientPickerState(
    val selectedKeys: Set<String>,
    val availableOptions: List<RecipeFilterOption>
)

class RecipeFilterController(
    initialState: RecipeFilterState = RecipeFilterState()
) {

    var state: RecipeFilterState = initialState
        private set

    private var ingredientOptions: List<RecipeFilterOption> = emptyList()

    val hasActiveFilters: Boolean
        get() = state.hasActiveFilters()

    fun clear(): RecipeFilterState {
        state = RecipeFilterState()
        return state
    }

    fun setIngredientOptions(options: List<RecipeFilterOption>) {
        ingredientOptions = options
    }

    fun selectMainCategory(key: String?): RecipeFilterState {
        state = state.copy(
            selectedMainCategoryKey = key,
            selectedSubcategoryKey = null
        )
        return state
    }

    fun selectSubcategory(key: String?): RecipeFilterState {
        state = state.copy(selectedSubcategoryKey = key)
        return state
    }

    fun selectMainIngredients(keys: Set<String>): RecipeFilterState {
        state = state.copy(selectedMainIngredientKeys = keys)
        return state
    }

    fun selectDiets(keys: Set<String>): RecipeFilterState {
        state = state.copy(selectedDietKeys = keys)
        return state
    }

    fun selectMethods(keys: Set<String>): RecipeFilterState {
        state = state.copy(selectedMethodKeys = keys)
        return state
    }

    fun selectDifficulty(key: String?): RecipeFilterState {
        state = state.copy(selectedDifficultyKey = key)
        return state
    }

    fun selectDuration(key: String?): RecipeFilterState {
        state = state.copy(selectedDurationKey = key)
        return state
    }

    fun ingredientPickerState(mode: RecipeIngredientFilterMode): RecipeIngredientPickerState {
        val selectedKeys = selectedIngredientKeys(mode)
        val blockedKeys = selectedIngredientKeys(mode.opposite())
        return RecipeIngredientPickerState(
            selectedKeys = selectedKeys,
            availableOptions = ingredientOptions.filterNot { it.key in blockedKeys }
        )
    }

    fun filterIngredientOptions(
        options: List<RecipeFilterOption>,
        query: String
    ): List<RecipeFilterOption> {
        val normalizedQuery = query.normalizeRecipeText()
        if (normalizedQuery.isBlank()) return options

        return options.filter { option ->
            option.label.normalizeRecipeText().contains(normalizedQuery) ||
                    option.key.normalizeRecipeText().contains(normalizedQuery)
        }
    }

    fun applyIngredientSelection(
        mode: RecipeIngredientFilterMode,
        selectedKeys: Set<String>
    ): RecipeFilterState {
        state = when (mode) {
            RecipeIngredientFilterMode.INCLUDE -> state.copy(
                selectedIncludedIngredientKeys = selectedKeys,
                selectedExcludedIngredientKeys = state.selectedExcludedIngredientKeys - selectedKeys
            )

            RecipeIngredientFilterMode.EXCLUDE -> state.copy(
                selectedExcludedIngredientKeys = selectedKeys,
                selectedIncludedIngredientKeys = state.selectedIncludedIngredientKeys - selectedKeys
            )
        }
        return state
    }

    fun removeIngredient(
        mode: RecipeIngredientFilterMode,
        key: String
    ): RecipeFilterState {
        state = when (mode) {
            RecipeIngredientFilterMode.INCLUDE -> state.copy(
                selectedIncludedIngredientKeys = state.selectedIncludedIngredientKeys - key
            )

            RecipeIngredientFilterMode.EXCLUDE -> state.copy(
                selectedExcludedIngredientKeys = state.selectedExcludedIngredientKeys - key
            )
        }
        return state
    }

    fun selectedIngredientOptions(mode: RecipeIngredientFilterMode): List<RecipeFilterOption> {
        val optionsByKey = ingredientOptions.associateBy { it.key }
        return selectedIngredientKeys(mode).map { key ->
            optionsByKey[key] ?: RecipeFilterOption(key = key, label = key)
        }
    }

    private fun selectedIngredientKeys(mode: RecipeIngredientFilterMode): Set<String> {
        return when (mode) {
            RecipeIngredientFilterMode.INCLUDE -> state.selectedIncludedIngredientKeys
            RecipeIngredientFilterMode.EXCLUDE -> state.selectedExcludedIngredientKeys
        }
    }

    private fun RecipeIngredientFilterMode.opposite(): RecipeIngredientFilterMode {
        return when (this) {
            RecipeIngredientFilterMode.INCLUDE -> RecipeIngredientFilterMode.EXCLUDE
            RecipeIngredientFilterMode.EXCLUDE -> RecipeIngredientFilterMode.INCLUDE
        }
    }
}
