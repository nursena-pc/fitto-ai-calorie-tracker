package com.example.fitto.feature_home.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeNotificationRouteResolverTest {

    @Test
    fun `resolve preserves social notification payload`() {
        val request = HomeNavigationRequest.SocialNotification(
            type = "post_comment",
            targetId = "post-8",
            secondaryTargetId = "comment-3"
        )

        assertEquals(
            HomeNotificationRouteResolver.Destination.Social(
                type = "post_comment",
                targetId = "post-8",
                secondaryTargetId = "comment-3"
            ),
            HomeNotificationRouteResolver.resolve(request)
        )
    }

    @Test
    fun `resolve sends weight reminders to profile weight`() {
        assertEquals(
            HomeNotificationRouteResolver.Destination.ProfileWeight,
            HomeNotificationRouteResolver.resolve(
                HomeNavigationRequest.ReminderNotification(
                    categoryName = "WEIGHT",
                    reminderId = "weight"
                )
            )
        )
    }

    @Test
    fun `resolve sends daily reminder categories to daily dashboard`() {
        listOf("MEAL", "WATER", "EXERCISE", "CUSTOM", "ALL", "unknown", null)
            .forEach { categoryName ->
                assertEquals(
                    HomeNotificationRouteResolver.Destination.DailyReminder(
                        categoryName = categoryName,
                        reminderId = "target-id"
                    ),
                    HomeNotificationRouteResolver.resolve(
                        HomeNavigationRequest.ReminderNotification(
                            categoryName = categoryName,
                            reminderId = "target-id"
                        )
                    )
                )
            }
    }
}
