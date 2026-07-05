package com.example.fitto.feature_home.presentation.social

internal enum class NotificationScreenTab {
    ALL,
    SOCIAL
}

internal data class NotificationsScreenState(
    val socialNotifications: List<FittoNotification> = emptyList(),
    val latestThreads: List<MessageThreadUiModel> = emptyList(),
    val selectedTab: NotificationScreenTab = NotificationScreenTab.ALL
)

/** Resolves notification list state independently from the DialogFragment view lifecycle. */
internal class NotificationsScreenStateStore(
    initialState: NotificationsScreenState = NotificationsScreenState()
) {
    var state: NotificationsScreenState = initialState
        private set

    fun updateSocialNotifications(notifications: List<FittoNotification>) {
        state = state.copy(socialNotifications = notifications)
    }

    fun updateThreads(threads: List<MessageThreadUiModel>) {
        state = state.copy(latestThreads = threads)
    }

    fun selectTab(tab: NotificationScreenTab) {
        state = state.copy(selectedTab = tab)
    }

    fun readThreadIds(): Set<String> {
        return state.latestThreads
            .asSequence()
            .filter { thread -> thread.unreadCount == 0 }
            .map { thread -> thread.id }
            .toSet()
    }

    fun allNotifications(localNotifications: List<FittoNotification>): List<FittoNotification> {
        val readThreadIds = readThreadIds()
        return (state.socialNotifications + localNotifications)
            .distinctBy { notification -> notification.id }
            .filterNot { notification ->
                isMessageNotification(notification) && notification.targetId in readThreadIds
            }
            .sortedByDescending { notification -> notification.createdAt }
    }

    fun visibleNotifications(localNotifications: List<FittoNotification>): List<FittoNotification> {
        val notifications = allNotifications(localNotifications)
        return when (state.selectedTab) {
            NotificationScreenTab.ALL -> notifications
            NotificationScreenTab.SOCIAL -> notifications.filter(::isSocialNotification)
        }
    }

    fun isMessageNotification(notification: FittoNotification): Boolean {
        return notification.type == FittoNotification.TYPE_MESSAGE ||
            notification.type == FittoNotification.TYPE_GROUP_MESSAGE ||
            notification.type == FittoNotification.TYPE_GROUP_ADDED
    }

    fun isSocialNotification(notification: FittoNotification): Boolean {
        return notification.type == FittoNotification.TYPE_POST_LIKE ||
            notification.type == FittoNotification.TYPE_POST_COMMENT ||
            notification.type == FittoNotification.TYPE_FOLLOWING_POST ||
            notification.type == FittoNotification.TYPE_COMMENT_REPLY ||
            notification.type == FittoNotification.TYPE_COMMENT_LIKE ||
            notification.type == FittoNotification.TYPE_FOLLOW ||
            notification.type == FittoNotification.TYPE_UNFOLLOW ||
            isMessageNotification(notification)
    }
}
