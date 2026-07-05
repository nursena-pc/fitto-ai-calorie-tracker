package com.example.fitto

import com.example.fitto.feature_home.presentation.navigation.HomeNavigationRequest
import com.example.fitto.feature_home.presentation.social.FittoNotification

internal data class MainNotificationIntentData(
    val openedSocialNotification: Boolean = false,
    val socialType: String? = null,
    val socialTargetId: String? = null,
    val socialSecondaryTargetId: String? = null,
    val openedReminderNotification: Boolean = false,
    val reminderCategoryName: String? = null,
    val reminderId: String? = null
)

internal data class MainNotificationIntentResolution(
    val navigationRequests: List<HomeNavigationRequest>,
    val messageThreadIdsToClear: List<String>
)

internal object MainNotificationIntentResolver {

    fun resolve(data: MainNotificationIntentData): MainNotificationIntentResolution {
        val requests = buildList {
            if (data.openedSocialNotification) {
                add(
                    HomeNavigationRequest.SocialNotification(
                        type = data.socialType,
                        targetId = data.socialTargetId,
                        secondaryTargetId = data.socialSecondaryTargetId
                    )
                )
            }

            if (data.openedReminderNotification) {
                add(
                    HomeNavigationRequest.ReminderNotification(
                        categoryName = data.reminderCategoryName,
                        reminderId = data.reminderId
                    )
                )
            }
        }

        val messageThreadIdsToClear = data.socialTargetId
            ?.takeIf { data.openedSocialNotification && data.socialType.isMessageNotificationType() }
            ?.takeIf { it.isNotBlank() }
            ?.let(::listOf)
            .orEmpty()

        return MainNotificationIntentResolution(
            navigationRequests = requests,
            messageThreadIdsToClear = messageThreadIdsToClear
        )
    }

    private fun String?.isMessageNotificationType(): Boolean {
        return this == FittoNotification.TYPE_MESSAGE ||
                this == FittoNotification.TYPE_GROUP_MESSAGE ||
                this == FittoNotification.TYPE_GROUP_ADDED
    }
}
