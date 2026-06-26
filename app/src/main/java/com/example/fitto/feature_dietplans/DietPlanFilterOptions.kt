package com.example.fitto.feature_dietplans

object DietPlanFilterOptions {
    const val GOAL_WEIGHT_LOSS = "weight_loss"
    const val GOAL_WEIGHT_GAIN = "weight_gain"
    const val GOAL_MUSCLE_GAIN = "muscle_gain"
    const val GOAL_BALANCED = "balanced"

    const val TYPE_MEDITERRANEAN = "mediterranean"
    const val TYPE_DASH = "dash"
    const val TYPE_INTERMITTENT_FASTING = "intermittent_fasting"
    const val TYPE_KETOGENIC = "ketogenic"
    const val TYPE_PLANT_BASED = "plant_based"
    const val TYPE_CUSTOM = "custom"

    const val FEATURE_HEART_FRIENDLY = "heart_friendly"
    const val FEATURE_LOW_SODIUM = "low_sodium"
    const val FEATURE_HIGH_PROTEIN = "high_protein"
    const val FEATURE_EASY = "easy"

    const val DEFAULT_GOAL = GOAL_WEIGHT_LOSS
    const val DEFAULT_DURATION = 14
    const val DEFAULT_TYPE = TYPE_MEDITERRANEAN
    const val DEFAULT_MIN_CALORIES = 1400
    const val DEFAULT_MAX_CALORIES = 2200

    val DEFAULT_FEATURES = setOf(FEATURE_HEART_FRIENDLY)
}
