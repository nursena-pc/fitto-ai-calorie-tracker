package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecipeCommunityBookRequest(
    val postId: String,
    val postUserId: String,
    val recipeId: String,
    val recipeTitle: String,
    val recipeImageResId: Int,
    val recipeDuration: String,
    val recipeServing: String,
    val recipeCalories: Int?,
    val recipeCategory: String,
    val recipeSourceTag: String,
    val recipeDescription: String,
    val recipeIngredients: List<String>,
    val recipeSteps: List<String>
)

data class RecipeCommunityShareRequest(
    val recipeId: String,
    val ingredients: List<String>,
    val comment: String
)

data class RecipeBookCopyRequest(
    val recipeId: String,
    val ingredientsOverride: List<String>?
)

interface RecipeDetailDataSource {
    fun findById(recipeId: String): RecipeUiModel?
    fun saveFavorite(recipeId: String)
    fun saveCommunityRecipe(request: RecipeCommunityBookRequest): String?
    suspend fun shareRecipeToCommunity(request: RecipeCommunityShareRequest): String?
    fun copyRecipeToUserBook(request: RecipeBookCopyRequest): String?
}

class RecipeDetailCoordinator(
    private val dataSource: RecipeDetailDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun loadRecipe(recipeId: String?): RecipeUiModel? {
        val id = recipeId ?: return null
        if (id.isBlank()) return null
        return withContext(ioDispatcher) { dataSource.findById(id) }
    }

    suspend fun saveOriginalRecipe(recipeId: String) {
        withContext(ioDispatcher) { dataSource.saveFavorite(recipeId) }
    }

    suspend fun saveCommunityRecipe(request: RecipeCommunityBookRequest): String? {
        return withContext(ioDispatcher) { dataSource.saveCommunityRecipe(request) }
    }

    suspend fun shareRecipeToCommunity(request: RecipeCommunityShareRequest): String? {
        return withContext(ioDispatcher) { dataSource.shareRecipeToCommunity(request) }
    }

    suspend fun copyRecipeToUserBook(request: RecipeBookCopyRequest): String? {
        return withContext(ioDispatcher) { dataSource.copyRecipeToUserBook(request) }
    }
}
