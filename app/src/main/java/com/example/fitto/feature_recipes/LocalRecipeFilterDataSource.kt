package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeFilterOption

class LocalRecipeFilterDataSource(
    context: Context
) : RecipeFilterDataSource {

    private val repository = RecipeRepository(context.applicationContext)

    override fun allIngredientOptions(): List<RecipeFilterOption> {
        return repository.allIngredientOptions()
    }
}
