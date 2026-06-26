package com.example.fitto.feature_recipes

data class RecipeDetailIngredient(
    val id: String,
    val icon: String,
    val name: String,
    val amountUnit: String,
    val estimatedCalories: Int
)
