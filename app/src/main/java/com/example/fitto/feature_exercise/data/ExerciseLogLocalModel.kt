package com.example.fitto.feature_exercise.data

data class ExerciseLogLocalModel(
    val id: String,
    val entryDate: String,
    val exerciseId: String,
    val exerciseTitle: String,
    val categoryName: String,
    val durationMinutes: Int,
    val intensityName: String,
    val metValue: Double,
    val calories: Int,
    val createdAt: Long
)

data class ExerciseDailySummary(
    val totalCalories: Int,
    val totalDurationMinutes: Int,
    val logCount: Int
)