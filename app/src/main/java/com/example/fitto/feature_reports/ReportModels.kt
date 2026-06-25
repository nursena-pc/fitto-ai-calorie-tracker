package com.example.fitto.feature_reports

import java.time.LocalDate

data class ReportDay(
    val date: LocalDate,
    val label: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
