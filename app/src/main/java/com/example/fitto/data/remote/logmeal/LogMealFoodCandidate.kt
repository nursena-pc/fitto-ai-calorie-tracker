package com.example.fitto.data.remote.logmeal

import java.io.Serializable

data class LogMealFoodCandidate(
    val name: String,
    val confidence: Double,
    val foodItemPosition: Int?,
    val servingGrams: Double?,
    val calories: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val fiber: Double?
) : Serializable {

    fun hasNutrition(): Boolean {
        return (calories ?: 0.0) > 0.0 ||
                (protein ?: 0.0) > 0.0 ||
                (carbs ?: 0.0) > 0.0 ||
                (fat ?: 0.0) > 0.0
    }
}