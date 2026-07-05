package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeUiModel

class LocalFridgeRecipeSuggestionDataSource(
    private val repository: RecipeRepository
) : FridgeRecipeSuggestionDataSource {

    override fun recipes(): List<RecipeUiModel> = repository.recipes()
}
