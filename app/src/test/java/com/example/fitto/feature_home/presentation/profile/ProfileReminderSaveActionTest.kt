package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileReminderSaveActionTest {

    @Test
    fun `save persists reminder before scheduling alarm`() {
        val effects = mutableListOf<String>()
        val action = ProfileReminderSaveAction(
            persistReminder = { effects += "persist:${it.id}" },
            scheduleReminder = { effects += "schedule:${it.id}" }
        )

        action.execute(reminderItem(id = "water"))

        assertEquals(listOf("persist:water", "schedule:water"), effects)
    }
}
