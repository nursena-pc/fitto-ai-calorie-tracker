package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeUiModel

class LocalFridgeRecipeSuggestionDataSource(
    context: Context
) : FridgeRecipeSuggestionDataSource {

    private val repository = RecipeRepository(context.applicationContext)

    override fun recipes(): List<RecipeUiModel> = repository.recipes()
}
