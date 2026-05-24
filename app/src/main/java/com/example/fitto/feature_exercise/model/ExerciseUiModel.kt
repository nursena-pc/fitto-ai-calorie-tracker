package com.example.fitto.feature_exercise.model

data class ExerciseUiModel(
    val id: String,
    val title: String,
    val description: String,
    val category: ExerciseCategory,
    val metValue: Double,
    val defaultDurationMinutes: Int,
    val iconEmoji: String,
    val searchKeywords: List<String> = emptyList(),
    val sourceCode: String = "",
    val sourceMajorHeading: String = "",
    val sourceDescription: String = ""
)
