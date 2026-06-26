package com.example.fitto.feature_dietplans

enum class DietPlanTab {
    READY,
    MY,
    DRAFTS,
    SAVED
}

data class DietPlanFilterState(
    val goal: String,
    val duration: Int,
    val type: String,
    val features: Set<String>,
    val minCalories: Int,
    val maxCalories: Int
)

data class PersonalizedDietRecommendation(
    val planId: String,
    val titleText: String,
    val reasonRes: Int,
    val matchPercent: Int,
    val chip1Res: Int,
    val chip2Res: Int,
    val chip3Res: Int
)
