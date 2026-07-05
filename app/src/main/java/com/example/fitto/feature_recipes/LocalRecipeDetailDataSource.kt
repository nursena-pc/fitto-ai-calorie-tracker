package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeUiModel

class LocalRecipeDetailDataSource(
    private val repository: RecipeRepository
) : RecipeDetailDataSource {

    override fun findById(recipeId: String): RecipeUiModel? {
        return repository.findById(recipeId)
    }

    override fun saveFavorite(recipeId: String) {
        repository.saveFavorite(recipeId)
    }

    override fun saveCommunityRecipe(request: RecipeCommunityBookRequest): String? {
        return repository.saveRecipeFromCommunityPost(
            postId = request.postId,
            postUserId = request.postUserId,
            recipeId = request.recipeId,
            recipeTitle = request.recipeTitle,
            recipeImageResId = request.recipeImageResId,
            recipeDuration = request.recipeDuration,
            recipeServing = request.recipeServing,
            recipeCalories = request.recipeCalories,
            recipeCategory = request.recipeCategory,
            recipeSourceTag = request.recipeSourceTag,
            recipeDescription = request.recipeDescription,
            recipeIngredients = request.recipeIngredients,
            recipeSteps = request.recipeSteps
        )
    }

    override suspend fun shareRecipeToCommunity(request: RecipeCommunityShareRequest): String? {
        return repository.shareRecipeToCommunity(
            recipeId = request.recipeId,
            ingredientsOverride = request.ingredients,
            comment = request.comment
        )
    }

    override fun copyRecipeToUserBook(request: RecipeBookCopyRequest): String? {
        return repository.copyRecipeToUserBook(
            recipeId = request.recipeId,
            ingredientsOverride = request.ingredientsOverride
        )
    }
}
