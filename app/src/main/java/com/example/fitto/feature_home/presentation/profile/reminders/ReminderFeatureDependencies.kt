package com.example.fitto.feature_home.presentation.profile.reminders

import android.content.Context
import com.example.fitto.feature_home.presentation.profile.ProfileReminderListAction
import com.example.fitto.feature_home.presentation.profile.ProfileRemindersBinder
import com.example.fitto.feature_home.presentation.profile.ProfileRemindersState
import com.example.fitto.feature_home.presentation.profile.ProfileRemindersStateStore
import com.example.fitto.feature_home.presentation.profile.ReminderPreferenceStore
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler

/** Reminder screen dependency graph owned by the application composition root. */
internal class ReminderFeatureDependencies(
    context: Context
) {
    private val appContext = context.applicationContext

    fun createBinder(
        initialCategory: ReminderCategory = ReminderCategory.ALL
    ): ProfileRemindersBinder {
        return ProfileRemindersBinder(
            listActionFactory = { context -> createListAction(context) },
            stateStore = ProfileRemindersStateStore(
                ProfileRemindersState(selectedCategory = initialCategory)
            )
        )
    }

    private fun createListAction(context: Context): ProfileReminderListAction {
        return ProfileReminderListAction(
            loadReminders = { ReminderPreferenceStore.getReminders(context) },
            persistEnabled = { id, enabled ->
                ReminderPreferenceStore.setEnabled(context, id, enabled)
            },
            deleteReminder = { id ->
                ReminderPreferenceStore.deleteReminder(context, id)
            },
            scheduleReminder = { item ->
                ReminderAlarmScheduler.scheduleReminder(appContext, item)
            },
            cancelReminder = { id ->
                ReminderAlarmScheduler.cancelReminder(appContext, id)
            }
        )
    }
}
