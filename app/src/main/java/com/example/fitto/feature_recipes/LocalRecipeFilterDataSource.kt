package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeFilterOption

class LocalRecipeFilterDataSource(
    private val repository: RecipeRepository
) : RecipeFilterDataSource {

    override fun allIngredientOptions(): List<RecipeFilterOption> {
        return repository.allIngredientOptions()
    }
}
