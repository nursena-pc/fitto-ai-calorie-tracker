package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem

class ProfileReminderSaveAction(
    private val persistReminder: (ProfileReminderItem) -> Unit,
    private val scheduleReminder: (ProfileReminderItem) -> Unit
) {

    fun execute(item: ProfileReminderItem) {
        persistReminder(item)
        scheduleReminder(item)
    }
}
