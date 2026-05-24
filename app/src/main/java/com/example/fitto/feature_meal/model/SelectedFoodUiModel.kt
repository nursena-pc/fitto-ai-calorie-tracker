package com.example.fitto.feature_meal.model

data class SelectedFoodUiModel(
    val id: String,
    val foodId: String,
    val name: String,
    val amountLabel: String,
    val amountGrams: Double,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val emoji: String
)
