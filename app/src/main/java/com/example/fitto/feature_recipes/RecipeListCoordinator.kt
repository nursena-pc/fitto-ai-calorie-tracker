package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface RecipeListDataSource {
    fun filterRecipes(query: String, filterState: RecipeFilterState): List<RecipeUiModel>
    fun toggleFavorite(recipeId: String)
}

class RecipeListCoordinator(
    private val dataSource: RecipeListDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun load(request: RecipeListLoadRequest): List<RecipeUiModel> {
        return withContext(ioDispatcher) {
            dataSource.filterRecipes(
                query = request.query,
                filterState = request.filterState
            )
        }
    }

    fun toggleFavorite(recipeId: String) {
        dataSource.toggleFavorite(recipeId)
    }
}

class RepositoryRecipeListDataSource(
    private val repository: RecipeRepository
) : RecipeListDataSource {
    override fun filterRecipes(
        query: String,
        filterState: RecipeFilterState
    ): List<RecipeUiModel> {
        return repository.filterRecipes(query, filterState)
    }

    override fun toggleFavorite(recipeId: String) {
        repository.toggleFavorite(recipeId)
    }
}
