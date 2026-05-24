package com.example.fitto.data.remote.usda

data class UsdaFoodProduct(
    val fdcId: Long,
    val description: String,
    val dataType: String,
    val brandOwner: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double?
)