package com.example.fitto.data.local.model

data class DailyMealSummary(
    val mealType: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)
