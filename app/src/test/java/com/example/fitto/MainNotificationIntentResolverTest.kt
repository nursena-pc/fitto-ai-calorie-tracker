package com.example.fitto

import com.example.fitto.feature_home.presentation.navigation.HomeNavigationRequest
import com.example.fitto.feature_home.presentation.social.FittoNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNotificationIntentResolverTest {

    @Test
    fun `resolve ignores notification payloads without open flags`() {
        val resolution = MainNotificationIntentResolver.resolve(
            MainNotificationIntentData(
                socialType = FittoNotification.TYPE_MESSAGE,
                socialTargetId = "thread-1",
                reminderCategoryName = "WATER",
                reminderId = "water"
            )
        )

        assertTrue(resolution.navigationRequests.isEmpty())
        assertTrue(resolution.messageThreadIdsToClear.isEmpty())
    }

    @Test
    fun `resolve creates social request and clears opened message thread`() {
        val resolution = MainNotificationIntentResolver.resolve(
            MainNotificationIntentData(
                openedSocialNotification = true,
                socialType = FittoNotification.TYPE_GROUP_MESSAGE,
                socialTargetId = "group-42",
                socialSecondaryTargetId = "message-7"
            )
        )

        assertEquals(
            listOf(
                HomeNavigationRequest.SocialNotification(
                    type = FittoNotification.TYPE_GROUP_MESSAGE,
                    targetId = "group-42",
                    secondaryTargetId = "message-7"
                )
            ),
            resolution.navigationRequests
        )
        assertEquals(listOf("group-42"), resolution.messageThreadIdsToClear)
    }

    @Test
    fun `resolve does not clear non-message social target`() {
        val resolution = MainNotificationIntentResolver.resolve(
            MainNotificationIntentData(
                openedSocialNotification = true,
                socialType = FittoNotification.TYPE_POST_COMMENT,
                socialTargetId = "post-2"
            )
        )

        assertTrue(resolution.messageThreadIdsToClear.isEmpty())
    }

    @Test
    fun `resolve creates reminder request`() {
        val resolution = MainNotificationIntentResolver.resolve(
            MainNotificationIntentData(
                openedReminderNotification = true,
                reminderCategoryName = "MEAL",
                reminderId = "lunch"
            )
        )

        assertEquals(
            listOf(
                HomeNavigationRequest.ReminderNotification(
                    categoryName = "MEAL",
                    reminderId = "lunch"
                )
            ),
            resolution.navigationRequests
        )
    }

    @Test
    fun `resolve preserves social then reminder navigation order`() {
        val resolution = MainNotificationIntentResolver.resolve(
            MainNotificationIntentData(
                openedSocialNotification = true,
                socialType = FittoNotification.TYPE_FOLLOW,
                socialTargetId = "user-1",
                openedReminderNotification = true,
                reminderCategoryName = "WEIGHT",
                reminderId = "weight"
            )
        )

        assertEquals(
            listOf(
                HomeNavigationRequest.SocialNotification(
                    type = FittoNotification.TYPE_FOLLOW,
                    targetId = "user-1",
                    secondaryTargetId = null
                ),
                HomeNavigationRequest.ReminderNotification(
                    categoryName = "WEIGHT",
                    reminderId = "weight"
                )
            ),
            resolution.navigationRequests
        )
    }
}
