package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeListCoordinatorTest {

    @Test
    fun `load forwards immutable request to data source`() = runBlocking {
        val expected = listOf(recipe("one"))
        val source = FakeRecipeListDataSource(expected)
        val coordinator = RecipeListCoordinator(source, Dispatchers.Unconfined)
        val request = RecipeListLoadRequest(
            generation = 7L,
            query = "salata",
            filterState = RecipeFilterState(selectedDietKeys = setOf("vegan"))
        )

        val result = coordinator.load(request)

        assertEquals(expected, result)
        assertEquals("salata", source.query)
        assertEquals(request.filterState, source.filterState)
    }

    @Test
    fun `toggle favorite delegates recipe id`() {
        val source = FakeRecipeListDataSource(emptyList())
        val coordinator = RecipeListCoordinator(source, Dispatchers.Unconfined)

        coordinator.toggleFavorite("recipe_1")

        assertEquals("recipe_1", source.favoriteId)
    }

    private class FakeRecipeListDataSource(
        private val recipes: List<RecipeUiModel>
    ) : RecipeListDataSource {
        var query: String? = null
        var filterState: RecipeFilterState? = null
        var favoriteId: String? = null

        override fun filterRecipes(
            query: String,
            filterState: RecipeFilterState
        ): List<RecipeUiModel> {
            this.query = query
            this.filterState = filterState
            return recipes
        }

        override fun toggleFavorite(recipeId: String) {
            favoriteId = recipeId
        }
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
