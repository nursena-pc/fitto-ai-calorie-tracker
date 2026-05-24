package com.example.fitto.feature_recipes.model

import androidx.annotation.DrawableRes

data class RecipeUiModel(
    val id: String,
    val title: String,
    val description: String,
    val mainCategoryKey: String,
    val subcategoryKey: String,
    val subcategoryKeys: Set<String> = emptySet(),
    val mainIngredientKeys: Set<String>,
    val dietKeys: Set<String>,
    val methodKeys: Set<String>,
    val durationMinutes: Int,
    val durationLabel: String,
    val difficultyKey: String,
    val calories: Int,
    @DrawableRes val imageResId: Int,
    val servings: Int = 1,
    val servingDisplay: String = "",
    val proteinGrams: Int = 0,
    val carbGrams: Int = 0,
    val fatGrams: Int = 0,
    val ingredientNames: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val tip: String? = null,
    val isFavorite: Boolean = false
)
