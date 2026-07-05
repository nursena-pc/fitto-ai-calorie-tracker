package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeListMode
import com.example.fitto.feature_recipes.model.RecipeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipesScreenStateStoreTest {

    @Test
    fun `search filter and list mode are preserved in one state`() {
        val store = RecipesScreenStateStore()
        val filter = RecipeFilterState(selectedDietKeys = setOf("vegan"))

        assertTrue(store.updateSearchQuery("çorba"))
        store.setFilter(filter)
        val mode = store.toggleListMode()

        assertEquals("çorba", store.state.searchQuery)
        assertEquals(filter, store.state.filterState)
        assertEquals(RecipeListMode.LIST, mode)
        assertEquals(RecipeListMode.LIST, store.state.listMode)
    }

    @Test
    fun `selecting category resets advanced filters and requests warning`() {
        val store = RecipesScreenStateStore(
            RecipesScreenState(
                filterState = RecipeFilterState(
                    selectedMainCategoryKey = "breakfast",
                    selectedDietKeys = setOf("vegan")
                )
            )
        )

        val result = store.selectCategory(RecipeFilterOption("soup", "Çorba"))

        assertTrue(result.changed)
        assertTrue(result.shouldWarnAdvancedFiltersReset)
        assertEquals("soup", store.state.filterState.selectedMainCategoryKey)
        assertTrue(store.state.filterState.selectedDietKeys.isEmpty())
    }

    @Test
    fun `all category clears filters`() {
        val store = RecipesScreenStateStore(
            RecipesScreenState(
                filterState = RecipeFilterState(selectedMainCategoryKey = "soup")
            )
        )

        store.selectCategory(RecipeFilterOption(RecipeCategorySelectionKeys.ALL, "Tümü"))

        assertFalse(store.state.filterState.hasActiveFilters())
    }

    @Test
    fun `stale recipe result cannot replace latest content`() {
        val store = RecipesScreenStateStore()
        val stale = store.beginLoad()
        store.updateSearchQuery("new")
        val latest = store.beginLoad()
        val latestRecipes = listOf(recipe("latest"))

        assertTrue(store.applyRecipes(latest.generation, latestRecipes))
        assertFalse(store.applyRecipes(stale.generation, listOf(recipe("stale"))))
        assertEquals(latestRecipes, store.state.recipes)
        assertEquals(1, store.state.resultCount)
    }

    private fun recipe(id: String) = RecipeUiModel(
        id = id,
        title = id,
        description = "",
        mainCategoryKey = "main",
        subcategoryKey = "sub",
        mainIngredientKeys = emptySet(),
        dietKeys = emptySet(),
        methodKeys = emptySet(),
        durationMinutes = 10,
        durationLabel = "10",
        difficultyKey = "easy",
        calories = 100,
        imageResId = 0
    )
}
