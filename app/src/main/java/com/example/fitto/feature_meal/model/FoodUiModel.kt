package com.example.fitto.feature_meal.model

data class FoodUiModel(
    val id: String,
    val name: String,
    val serving: String,
    val calories: Int,
    val emoji: String,
    val defaultServingAmount: Double,
    val defaultUnit: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val isFavorite: Boolean = false
)