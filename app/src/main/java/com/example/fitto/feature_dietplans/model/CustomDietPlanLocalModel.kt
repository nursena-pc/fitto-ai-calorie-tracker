package com.example.fitto.feature_dietplans.model

data class CustomDietPlanLocalModel(
    val id: String,
    val title: String,
    val description: String,
    val goalType: String,
    val dailyCalories: Int,
    val mealCount: Int,
    val durationDays: Int,
    val isPublic: Boolean,
    val preferences: List<String>,
    val exclusions: List<String>,
    val mealOrder: List<String>,
    val isDraft: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)