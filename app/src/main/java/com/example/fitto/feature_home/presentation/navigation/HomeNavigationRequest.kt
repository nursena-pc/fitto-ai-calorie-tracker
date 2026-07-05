package com.example.fitto.feature_home.presentation.navigation

internal sealed interface HomeNavigationRequest {

    data class SocialNotification(
        val type: String?,
        val targetId: String?,
        val secondaryTargetId: String?
    ) : HomeNavigationRequest

    data class ReminderNotification(
        val categoryName: String?,
        val reminderId: String?
    ) : HomeNavigationRequest
}
