package com.example.fitto.feature_dietplans.model

data class ActiveDietPlanUiModel(
    val id: String,
    val title: String,
    val badge: String,
    val icon: String,
    val primaryLabel: String,
    val primaryValue: String,
    val secondaryLabel: String,
    val secondaryValue: String,
    val progress: Int,
    val kcalPerDay: Int,
    val durationDays: Int
)
