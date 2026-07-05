package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.UserRecipeDraft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RecipeAddDataSource {
    fun addUserRecipe(draft: UserRecipeDraft): String
}

class RecipeAddCoordinator(
    private val dataSource: RecipeAddDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun saveRecipe(draft: UserRecipeDraft): String {
        return withContext(ioDispatcher) { dataSource.addUserRecipe(draft) }
    }
}
