package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileReminderListActionTest {

    @Test
    fun `load filters selected category and keeps enabled reminders first`() {
        val fixture = fixture(
            reminders = listOf(
                reminderItem(
                    id = "disabled",
                    title = "A",
                    category = ReminderCategory.MEAL,
                    isEnabled = false,
                    minuteOfDay = 7 * 60
                ),
                reminderItem(
                    id = "enabled-late",
                    title = "B",
                    category = ReminderCategory.MEAL,
                    minuteOfDay = 12 * 60
                ),
                reminderItem(
                    id = "enabled-early",
                    title = "C",
                    category = ReminderCategory.MEAL,
                    minuteOfDay = 8 * 60
                ),
                reminderItem(
                    id = "water",
                    category = ReminderCategory.WATER
                )
            )
        )

        val result = fixture.action.load(ReminderCategory.MEAL)

        assertEquals(
            listOf("enabled-early", "enabled-late", "disabled"),
            result.items.map { it.id }
        )
        assertEquals(
            listOf("enabled-early", "enabled-late"),
            result.activeItems.map { it.id }
        )
    }

    @Test
    fun `water reminders sort by interval start minute`() {
        val fixture = fixture(
            reminders = listOf(
                reminderItem(
                    id = "later",
                    category = ReminderCategory.WATER,
                    isWaterInterval = true,
                    minuteOfDay = 6 * 60,
                    waterStartMinute = 11 * 60
                ),
                reminderItem(
                    id = "earlier",
                    category = ReminderCategory.WATER,
                    isWaterInterval = true,
                    minuteOfDay = 20 * 60,
                    waterStartMinute = 8 * 60
                )
            )
        )

        val result = fixture.action.load(ReminderCategory.ALL)

        assertEquals(listOf("earlier", "later"), result.items.map { it.id })
    }

    @Test
    fun `enabling reminder persists before scheduling enabled copy`() {
        val fixture = fixture()
        val item = reminderItem(id = "meal", isEnabled = false)

        fixture.action.updateEnabled(item, enabled = true)

        assertEquals(listOf("persist:meal:true", "schedule:meal:true"), fixture.effects)
    }

    @Test
    fun `disabling reminder persists before cancelling alarm`() {
        val fixture = fixture()
        val item = reminderItem(id = "meal")

        fixture.action.updateEnabled(item, enabled = false)

        assertEquals(listOf("persist:meal:false", "cancel:meal"), fixture.effects)
    }

    @Test
    fun `deleting reminder removes persisted item before cancelling alarm`() {
        val fixture = fixture()

        fixture.action.delete(reminderItem(id = "custom"))

        assertEquals(listOf("delete:custom", "cancel:custom"), fixture.effects)
    }

    private fun fixture(
        reminders: List<com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem> = emptyList()
    ): Fixture {
        val effects = mutableListOf<String>()
        val action = ProfileReminderListAction(
            loadReminders = { reminders },
            persistEnabled = { id, enabled -> effects += "persist:$id:$enabled" },
            deleteReminder = { id -> effects += "delete:$id" },
            scheduleReminder = { item -> effects += "schedule:${item.id}:${item.isEnabled}" },
            cancelReminder = { id -> effects += "cancel:$id" },
            titleSortKey = { it.lowercase() }
        )

        return Fixture(action, effects)
    }

    private data class Fixture(
        val action: ProfileReminderListAction,
        val effects: MutableList<String>
    )
}
