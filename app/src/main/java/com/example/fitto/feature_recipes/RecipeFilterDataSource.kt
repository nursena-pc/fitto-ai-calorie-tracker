package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption

interface RecipeFilterDataSource {
    fun allIngredientOptions(): List<RecipeFilterOption>
}
