package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeFilterCoordinator(
    private val dataSource: RecipeFilterDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun loadIngredientOptions(): List<RecipeFilterOption> {
        return withContext(ioDispatcher) { dataSource.allIngredientOptions() }
    }
}
