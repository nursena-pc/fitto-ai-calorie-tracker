package com.example.fitto.feature_health

data class HealthConnectDailySummary(
    val steps: Long = 0L,
    val activeCaloriesKcal: Int = 0,
    val hasPermission: Boolean = false,
    val isAvailable: Boolean = false
)