package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsScreenStateStoreTest {

    @Test
    fun allNotifications_deduplicatesAndSortsNewestFirst() {
        val store = NotificationsScreenStateStore()
        store.updateSocialNotifications(
            listOf(
                notification(id = "same", createdAt = 20L),
                notification(id = "social", createdAt = 10L)
            )
        )

        val result = store.allNotifications(
            listOf(
                notification(id = "same", createdAt = 5L),
                notification(id = "local", createdAt = 30L)
            )
        )

        assertEquals(listOf("local", "same", "social"), result.map { it.id })
        assertEquals(20L, result.first { it.id == "same" }.createdAt)
    }

    @Test
    fun allNotifications_hidesMessageNotificationForReadThread() {
        val store = NotificationsScreenStateStore()
        store.updateThreads(
            listOf(thread(id = "read", unreadCount = 0), thread(id = "unread", unreadCount = 2))
        )

        val result = store.allNotifications(
            listOf(
                notification(id = "1", type = FittoNotification.TYPE_MESSAGE, targetId = "read"),
                notification(id = "2", type = FittoNotification.TYPE_GROUP_MESSAGE, targetId = "unread")
            )
        )

        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun visibleNotifications_socialTabExcludesGeneralAndReminderItems() {
        val store = NotificationsScreenStateStore()
        store.selectTab(NotificationScreenTab.SOCIAL)

        val result = store.visibleNotifications(
            listOf(
                notification(id = "social", type = FittoNotification.TYPE_FOLLOW),
                notification(id = "message", type = FittoNotification.TYPE_MESSAGE),
                notification(id = "reminder", type = FittoNotification.TYPE_REMINDER),
                notification(id = "general", type = FittoNotification.TYPE_GENERAL)
            )
        )

        assertEquals(listOf("social", "message"), result.map { it.id })
    }

    @Test
    fun typePolicies_includeAllMessageVariantsInSocialNotifications() {
        val store = NotificationsScreenStateStore()

        val messageTypes = listOf(
            FittoNotification.TYPE_MESSAGE,
            FittoNotification.TYPE_GROUP_MESSAGE,
            FittoNotification.TYPE_GROUP_ADDED
        )

        messageTypes.forEach { type ->
            val notification = notification(id = type, type = type)
            assertTrue(store.isMessageNotification(notification))
            assertTrue(store.isSocialNotification(notification))
        }
        assertFalse(
            store.isSocialNotification(
                notification(id = "reminder", type = FittoNotification.TYPE_REMINDER)
            )
        )
    }

    private fun notification(
        id: String,
        type: String = FittoNotification.TYPE_FOLLOW,
        targetId: String? = null,
        createdAt: Long = 0L
    ): FittoNotification {
        return FittoNotification(
            id = id,
            title = "Title",
            description = "Description",
            iconRes = 0,
            type = type,
            targetId = targetId,
            createdAt = createdAt
        )
    }

    private fun thread(id: String, unreadCount: Int): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = id,
            title = "Thread",
            lastMessage = "",
            timeText = "",
            isGroup = false,
            initials = "T",
            unreadCount = unreadCount
        )
    }
}
