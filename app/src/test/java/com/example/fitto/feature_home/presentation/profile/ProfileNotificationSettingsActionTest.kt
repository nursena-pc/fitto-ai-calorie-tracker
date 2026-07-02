package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileNotificationSettingsActionTest {

    @Test
    fun `enabling master notifications schedules reminders when permission is available`() {
        val fixture = fixture(permissionGranted = true)

        val result = fixture.action.toggle(ProfileNotificationToggle.MASTER)

        assertTrue(result.enabled)
        assertTrue(result.permissionRequested)
        assertTrue(result.remindersScheduled)
        assertFalse(result.remindersCancelled)
        assertEquals(listOf("permission", "schedule"), fixture.effects)
    }

    @Test
    fun `enabling master notifications waits for permission before scheduling`() {
        val fixture = fixture(permissionGranted = false)

        val result = fixture.action.toggle(ProfileNotificationToggle.MASTER)

        assertTrue(result.enabled)
        assertTrue(result.permissionRequested)
        assertFalse(result.remindersScheduled)
        assertEquals(listOf("permission"), fixture.effects)
    }

    @Test
    fun `disabling master notifications cancels reminders without permission request`() {
        val fixture = fixture(
            initiallyEnabled = mapOf(ProfileNotificationToggle.MASTER to true)
        )

        val result = fixture.action.toggle(ProfileNotificationToggle.MASTER)

        assertFalse(result.enabled)
        assertFalse(result.permissionRequested)
        assertTrue(result.remindersCancelled)
        assertEquals(listOf("cancel"), fixture.effects)
    }

    @Test
    fun `enabling reminder notifications requests permission and schedules`() {
        val fixture = fixture(permissionGranted = true)

        val result = fixture.action.toggle(ProfileNotificationToggle.REMINDER)

        assertTrue(result.enabled)
        assertEquals(listOf("permission", "schedule"), fixture.effects)
    }

    @Test
    fun `disabling reminder notifications cancels all reminder alarms`() {
        val fixture = fixture(
            initiallyEnabled = mapOf(ProfileNotificationToggle.REMINDER to true)
        )

        val result = fixture.action.toggle(ProfileNotificationToggle.REMINDER)

        assertFalse(result.enabled)
        assertTrue(result.remindersCancelled)
        assertEquals(listOf("cancel"), fixture.effects)
    }

    @Test
    fun `enabling social notifications only requests permission`() {
        val fixture = fixture(permissionGranted = true)

        val result = fixture.action.toggle(ProfileNotificationToggle.SOCIAL)

        assertTrue(result.enabled)
        assertTrue(result.permissionRequested)
        assertFalse(result.remindersScheduled)
        assertEquals(listOf("permission"), fixture.effects)
    }

    @Test
    fun `disabling social notifications has no scheduler side effect`() {
        val fixture = fixture(
            initiallyEnabled = mapOf(ProfileNotificationToggle.SOCIAL to true)
        )

        val result = fixture.action.toggle(ProfileNotificationToggle.SOCIAL)

        assertFalse(result.enabled)
        assertFalse(result.permissionRequested)
        assertEquals(emptyList<String>(), fixture.effects)
    }

    @Test
    fun `direct and group message toggles share permission behavior`() {
        val fixture = fixture(permissionGranted = false)

        fixture.action.toggle(ProfileNotificationToggle.DIRECT_MESSAGE)
        fixture.action.toggle(ProfileNotificationToggle.GROUP_MESSAGE)

        assertEquals(listOf("permission", "permission"), fixture.effects)
        assertTrue(fixture.store.isEnabled(ProfileNotificationToggle.DIRECT_MESSAGE))
        assertTrue(fixture.store.isEnabled(ProfileNotificationToggle.GROUP_MESSAGE))
    }

    @Test
    fun `achievement toggle only persists the new value`() {
        val fixture = fixture()

        val result = fixture.action.toggle(ProfileNotificationToggle.ACHIEVEMENT)

        assertTrue(result.enabled)
        assertEquals(emptyList<String>(), fixture.effects)
    }

    @Test
    fun `vibration toggle only persists the new value`() {
        val fixture = fixture(
            initiallyEnabled = mapOf(ProfileNotificationToggle.VIBRATION to true)
        )

        val result = fixture.action.toggle(ProfileNotificationToggle.VIBRATION)

        assertFalse(result.enabled)
        assertEquals(emptyList<String>(), fixture.effects)
    }

    private fun fixture(
        initiallyEnabled: Map<ProfileNotificationToggle, Boolean> = emptyMap(),
        permissionGranted: Boolean = true
    ): Fixture {
        val store = FakeStore(initiallyEnabled)
        val effects = mutableListOf<String>()
        val action = ProfileNotificationSettingsAction(
            store = store,
            requestNotificationPermission = {
                effects += "permission"
                permissionGranted
            },
            scheduleReminders = { effects += "schedule" },
            cancelReminders = { effects += "cancel" }
        )
        return Fixture(action, store, effects)
    }

    private data class Fixture(
        val action: ProfileNotificationSettingsAction,
        val store: FakeStore,
        val effects: MutableList<String>
    )

    private class FakeStore(
        initiallyEnabled: Map<ProfileNotificationToggle, Boolean>
    ) : ProfileNotificationSettingsStore {

        private val values = ProfileNotificationToggle.entries
            .associateWith { toggle -> initiallyEnabled[toggle] ?: false }
            .toMutableMap()

        override fun isEnabled(toggle: ProfileNotificationToggle): Boolean {
            return values.getValue(toggle)
        }

        override fun setEnabled(toggle: ProfileNotificationToggle, enabled: Boolean) {
            values[toggle] = enabled
        }

        override fun notificationSound(): ProfileNotificationSound = ProfileNotificationSound.SOFT

        override fun setNotificationSound(sound: ProfileNotificationSound) = Unit

        override fun setCustomNotificationSound(uri: String, displayName: String) = Unit

        override fun silentHours(): String = "22:00 - 07:00"

        override fun setSilentHours(value: String) = Unit

        override fun markExactAlarmPromptSeen() = Unit
    }
}
