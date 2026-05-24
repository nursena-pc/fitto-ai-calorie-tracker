package com.example.fitto.feature_recipes.model

import android.os.Bundle

data class RecipeFilterState(
    val selectedMainCategoryKey: String? = null,
    val selectedSubcategoryKey: String? = null,
    val selectedMainIngredientKeys: Set<String> = emptySet(),
    val selectedDietKeys: Set<String> = emptySet(),
    val selectedMethodKeys: Set<String> = emptySet(),
    val selectedDurationKey: String? = null,
    val selectedDifficultyKey: String? = null,
    val selectedIncludedIngredientKeys: Set<String> = emptySet(),
    val selectedExcludedIngredientKeys: Set<String> = emptySet()
) {
    val activeFilterCount: Int
        get() = listOfNotNull(
            selectedMainCategoryKey,
            selectedSubcategoryKey,
            selectedDurationKey,
            selectedDifficultyKey
        ).size +
                selectedMainIngredientKeys.size +
                selectedDietKeys.size +
                selectedMethodKeys.size +
                selectedIncludedIngredientKeys.size +
                selectedExcludedIngredientKeys.size

    fun hasActiveFilters(): Boolean = activeFilterCount > 0

    fun hasAdvancedFilters(): Boolean {
        return selectedSubcategoryKey != null ||
                selectedMainIngredientKeys.isNotEmpty() ||
                selectedDietKeys.isNotEmpty() ||
                selectedMethodKeys.isNotEmpty() ||
                selectedDurationKey != null ||
                selectedDifficultyKey != null ||
                selectedIncludedIngredientKeys.isNotEmpty() ||
                selectedExcludedIngredientKeys.isNotEmpty()
    }

    fun clear(): RecipeFilterState = RecipeFilterState()

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_MAIN_CATEGORY, selectedMainCategoryKey)
        putString(KEY_SUBCATEGORY, selectedSubcategoryKey)
        putStringArray(KEY_MAIN_INGREDIENTS, selectedMainIngredientKeys.toTypedArray())
        putStringArray(KEY_DIETS, selectedDietKeys.toTypedArray())
        putStringArray(KEY_METHODS, selectedMethodKeys.toTypedArray())
        putString(KEY_DURATION, selectedDurationKey)
        putString(KEY_DIFFICULTY, selectedDifficultyKey)
        putStringArray(KEY_INCLUDED_INGREDIENTS, selectedIncludedIngredientKeys.toTypedArray())
        putStringArray(KEY_EXCLUDED_INGREDIENTS, selectedExcludedIngredientKeys.toTypedArray())
    }

    companion object {
        const val RESULT_REQUEST_KEY = "recipe_filter_result_request"
        const val RESULT_BUNDLE_KEY = "recipe_filter_state"

        private const val KEY_MAIN_CATEGORY = "main_category"
        private const val KEY_SUBCATEGORY = "subcategory"
        private const val KEY_MAIN_INGREDIENTS = "main_ingredients"
        private const val KEY_DIETS = "diets"
        private const val KEY_METHODS = "methods"
        private const val KEY_DURATION = "duration"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_INCLUDED_INGREDIENTS = "included_ingredients"
        private const val KEY_EXCLUDED_INGREDIENTS = "excluded_ingredients"

        fun fromBundle(bundle: Bundle?): RecipeFilterState {
            if (bundle == null) return RecipeFilterState()

            return RecipeFilterState(
                selectedMainCategoryKey = bundle.getString(KEY_MAIN_CATEGORY),
                selectedSubcategoryKey = bundle.getString(KEY_SUBCATEGORY),
                selectedMainIngredientKeys = bundle.getStringArray(KEY_MAIN_INGREDIENTS).orEmpty().toSet(),
                selectedDietKeys = bundle.getStringArray(KEY_DIETS).orEmpty().toSet(),
                selectedMethodKeys = bundle.getStringArray(KEY_METHODS).orEmpty().toSet(),
                selectedDurationKey = bundle.getString(KEY_DURATION),
                selectedDifficultyKey = bundle.getString(KEY_DIFFICULTY),
                selectedIncludedIngredientKeys = bundle.getStringArray(KEY_INCLUDED_INGREDIENTS).orEmpty().toSet(),
                selectedExcludedIngredientKeys = bundle.getStringArray(KEY_EXCLUDED_INGREDIENTS).orEmpty().toSet()
            )
        }
    }
}
