package com.example.fitto.feature_home.presentation.navigation

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory

internal object HomeNotificationRouteResolver {

    sealed interface Destination {
        data class Social(
            val type: String?,
            val targetId: String?,
            val secondaryTargetId: String?
        ) : Destination

        data object ProfileWeight : Destination

        data class DailyReminder(
            val categoryName: String?,
            val reminderId: String?
        ) : Destination
    }

    fun resolve(request: HomeNavigationRequest): Destination {
        return when (request) {
            is HomeNavigationRequest.SocialNotification -> {
                Destination.Social(
                    type = request.type,
                    targetId = request.targetId,
                    secondaryTargetId = request.secondaryTargetId
                )
            }

            is HomeNavigationRequest.ReminderNotification -> {
                if (parseReminderCategory(request.categoryName) == ReminderCategory.WEIGHT) {
                    Destination.ProfileWeight
                } else {
                    Destination.DailyReminder(
                        categoryName = request.categoryName,
                        reminderId = request.reminderId
                    )
                }
            }
        }
    }

    private fun parseReminderCategory(categoryName: String?): ReminderCategory? {
        return runCatching {
            ReminderCategory.valueOf(categoryName.orEmpty())
        }.getOrNull()
    }
}
