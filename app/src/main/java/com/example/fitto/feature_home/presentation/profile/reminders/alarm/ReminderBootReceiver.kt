package com.example.fitto.feature_home.presentation.profile.reminders.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationChannels
import com.example.fitto.feature_home.presentation.profile.ReminderPreferenceStore

class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val shouldRebuildAlarms = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> true
            else -> false
        }

        if (!shouldRebuildAlarms) return
        if (!ReminderPreferenceStore.hasSavedReminders(context)) return

        FittoNotificationChannels.ensureCreated(context)
        ReminderAlarmScheduler.scheduleAll(context)
    }
}
