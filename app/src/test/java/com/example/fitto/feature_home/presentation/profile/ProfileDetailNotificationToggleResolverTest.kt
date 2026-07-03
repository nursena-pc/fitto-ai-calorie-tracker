package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileDetailNotificationToggleResolverTest {

    @Test
    fun `notification actions map to their matching toggle`() {
        val expected = mapOf(
            ProfileDetailAction.TOGGLE_NOTIFICATIONS_ALLOWED to ProfileNotificationToggle.MASTER,
            ProfileDetailAction.TOGGLE_ACHIEVEMENT_NOTIFICATIONS to ProfileNotificationToggle.ACHIEVEMENT,
            ProfileDetailAction.TOGGLE_REMINDER_NOTIFICATIONS to ProfileNotificationToggle.REMINDER,
            ProfileDetailAction.TOGGLE_SOCIAL_NOTIFICATIONS to ProfileNotificationToggle.SOCIAL,
            ProfileDetailAction.TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS to ProfileNotificationToggle.DIRECT_MESSAGE,
            ProfileDetailAction.TOGGLE_GROUP_MESSAGE_NOTIFICATIONS to ProfileNotificationToggle.GROUP_MESSAGE,
            ProfileDetailAction.TOGGLE_VIBRATION to ProfileNotificationToggle.VIBRATION
        )

        expected.forEach { (action, toggle) ->
            assertEquals(
                toggle,
                ProfileDetailNotificationToggleResolver.resolve(action)
            )
        }
    }

    @Test
    fun `non notification actions do not resolve a toggle`() {
        assertNull(
            ProfileDetailNotificationToggleResolver.resolve(
                ProfileDetailAction.EDIT_EMAIL
            )
        )
        assertNull(ProfileDetailNotificationToggleResolver.resolve(null))
    }
}
