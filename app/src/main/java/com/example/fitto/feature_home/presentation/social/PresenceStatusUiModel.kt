package com.example.fitto.feature_home.presentation.social

data class PresenceStatusUiModel(
    val userId: String,
    val isOnline: Boolean,
    val lastChanged: Long
)
