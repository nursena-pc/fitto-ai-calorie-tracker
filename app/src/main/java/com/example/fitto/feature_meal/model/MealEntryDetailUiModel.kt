package com.example.fitto.feature_meal.model

data class MealEntryDetailUiModel(
    val entryId: String,
    val title: String,
    val sourceType: String,
    val foodSummary: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val createdAt: Long,
    val foodCount: Int
)
