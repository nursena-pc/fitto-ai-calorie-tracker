package com.example.fitto.feature_meal.model

data class MealEntryFoodUiModel(
    val id: String,
    val mealEntryId: String,
    val foodId: String?,
    val foodName: String,
    val amountLabel: String,
    val amountGrams: Double,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)