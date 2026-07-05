package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeListMode
import com.example.fitto.feature_recipes.model.RecipeUiModel

object RecipeCategorySelectionKeys {
    const val ALL = "all"
}

data class RecipesScreenState(
    val filterState: RecipeFilterState = RecipeFilterState(),
    val searchQuery: String = "",
    val listMode: RecipeListMode = RecipeListMode.GRID,
    val recipes: List<RecipeUiModel> = emptyList(),
    val contentGeneration: Long = 0L
) {
    val resultCount: Int get() = recipes.size
}

data class RecipeListLoadRequest(
    val generation: Long,
    val query: String,
    val filterState: RecipeFilterState
)

data class RecipeCategorySelectionResult(
    val changed: Boolean,
    val shouldWarnAdvancedFiltersReset: Boolean
)

class RecipesScreenStateStore(
    initialState: RecipesScreenState = RecipesScreenState()
) {
    var state: RecipesScreenState = initialState
        private set

    fun updateSearchQuery(query: String): Boolean {
        if (state.searchQuery == query) return false
        state = state.copy(searchQuery = query)
        return true
    }

    fun setFilter(filterState: RecipeFilterState) {
        state = state.copy(filterState = filterState)
    }

    fun selectCategory(option: RecipeFilterOption): RecipeCategorySelectionResult {
        val currentFilter = state.filterState
        val hadAdvancedFilters = currentFilter.hasAdvancedFilters()
        val newFilter = when {
            option.key == RecipeCategorySelectionKeys.ALL -> RecipeFilterState()
            currentFilter.selectedMainCategoryKey == option.key && !hadAdvancedFilters -> RecipeFilterState()
            else -> RecipeFilterState(selectedMainCategoryKey = option.key)
        }
        val changed = newFilter != currentFilter
        if (changed) state = state.copy(filterState = newFilter)
        return RecipeCategorySelectionResult(
            changed = changed,
            shouldWarnAdvancedFiltersReset = hadAdvancedFilters && changed
        )
    }

    fun clearFilters(): Boolean {
        if (!state.filterState.hasActiveFilters()) return false
        state = state.copy(filterState = RecipeFilterState())
        return true
    }

    fun toggleListMode(): RecipeListMode {
        val nextMode = when (state.listMode) {
            RecipeListMode.GRID -> RecipeListMode.LIST
            RecipeListMode.LIST -> RecipeListMode.GRID
        }
        state = state.copy(listMode = nextMode)
        return nextMode
    }

    fun beginLoad(): RecipeListLoadRequest {
        val generation = state.contentGeneration + 1L
        state = state.copy(contentGeneration = generation)
        return RecipeListLoadRequest(
            generation = generation,
            query = state.searchQuery,
            filterState = state.filterState
        )
    }

    fun applyRecipes(generation: Long, recipes: List<RecipeUiModel>): Boolean {
        if (generation != state.contentGeneration) return false
        state = state.copy(recipes = recipes)
        return true
    }
}
