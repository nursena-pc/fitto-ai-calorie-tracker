package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.data.UserRecipeDraft

class LocalRecipeAddDataSource(
    private val repository: RecipeRepository
) : RecipeAddDataSource {

    override fun addUserRecipe(draft: UserRecipeDraft): String {
        return repository.addUserRecipe(draft)
    }
}
