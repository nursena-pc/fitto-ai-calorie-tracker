package com.example.fitto.feature_sleep.data

data class SleepLogLocalModel(
    val date: String,
    val bedtimeHour: Int,
    val bedtimeMinute: Int,
    val wakeHour: Int,
    val wakeMinute: Int,
    val durationMinutes: Int,
    val caloriesBurned: Int
)