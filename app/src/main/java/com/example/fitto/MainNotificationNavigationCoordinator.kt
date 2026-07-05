package com.example.fitto

import android.content.Intent
import com.example.fitto.feature_home.presentation.navigation.HomeNavigationRequest
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPublisher
import com.example.fitto.feature_home.presentation.social.FittoNotification

internal class MainNotificationNavigationCoordinator(
    private val cancelActiveSystemNotifications: () -> Unit,
    private val markLocalNotificationRead: (String) -> Unit,
    private val removeMessageNotificationsForThread: (String) -> Unit,
    private val navigate: (HomeNavigationRequest) -> Unit
) {

    fun handleLaunchIntent(intent: Intent?) {
        cancelActiveSystemNotifications()

        val resolution = MainNotificationIntentResolver.resolve(intent.toNotificationIntentData())
        resolution.messageThreadIdsToClear.forEach(removeMessageNotificationsForThread)
        resolution.navigationRequests.forEach(navigate)

        intent.consumeNotificationExtras()
    }

    fun openInAppNotification(notification: FittoNotification) {
        markLocalNotificationRead(notification.id)
        if (notification.type.isMessageNotificationType()) {
            notification.targetId?.let(removeMessageNotificationsForThread)
        }

        MainNotificationIntentResolver.resolve(
            MainNotificationIntentData(
                openedSocialNotification = true,
                socialType = notification.type,
                socialTargetId = notification.targetId,
                socialSecondaryTargetId = notification.secondaryTargetId
            )
        ).navigationRequests.forEach(navigate)
    }

    private fun String?.isMessageNotificationType(): Boolean {
        return this == FittoNotification.TYPE_MESSAGE ||
                this == FittoNotification.TYPE_GROUP_MESSAGE ||
                this == FittoNotification.TYPE_GROUP_ADDED
    }

    private fun Intent?.toNotificationIntentData(): MainNotificationIntentData {
        return MainNotificationIntentData(
            openedSocialNotification = this?.getBooleanExtra(
                FittoNotificationPublisher.EXTRA_OPENED_SOCIAL_NOTIFICATION,
                false
            ) == true,
            socialType = this?.getStringExtra(
                FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TYPE
            ),
            socialTargetId = this?.getStringExtra(
                FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TARGET_ID
            ),
            socialSecondaryTargetId = this?.getStringExtra(
                FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID
            ),
            openedReminderNotification = this?.getBooleanExtra(
                FittoNotificationPublisher.EXTRA_OPENED_REMINDER_NOTIFICATION,
                false
            ) == true,
            reminderCategoryName = this?.getStringExtra(
                FittoNotificationPublisher.EXTRA_REMINDER_CATEGORY
            ),
            reminderId = this?.getStringExtra(
                FittoNotificationPublisher.EXTRA_REMINDER_ID
            )
        )
    }

    private fun Intent?.consumeNotificationExtras() {
        this ?: return

        if (getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_FROM_NOTIFICATION, false)) {
            removeExtra(FittoNotificationPublisher.EXTRA_OPENED_FROM_NOTIFICATION)
        }

        if (getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_SOCIAL_NOTIFICATION, false)) {
            removeExtra(FittoNotificationPublisher.EXTRA_OPENED_SOCIAL_NOTIFICATION)
            removeExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TYPE)
            removeExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TARGET_ID)
            removeExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID)
        }

        if (getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_REMINDER_NOTIFICATION, false)) {
            removeExtra(FittoNotificationPublisher.EXTRA_OPENED_REMINDER_NOTIFICATION)
            removeExtra(FittoNotificationPublisher.EXTRA_REMINDER_ID)
            removeExtra(FittoNotificationPublisher.EXTRA_REMINDER_CATEGORY)
        }
    }
}
