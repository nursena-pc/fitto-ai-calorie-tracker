package com.example.fitto.feature_home.presentation.profile.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationChannels

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderAlarmScheduler.ACTION_TRIGGER_REMINDER) return

        val pendingResult = goAsync()

        try {
            val reminderId = intent.getStringExtra(ReminderAlarmScheduler.EXTRA_REMINDER_ID)
                ?: return
            val occurrenceMinute = intent.getIntExtra(
                ReminderAlarmScheduler.EXTRA_OCCURRENCE_MINUTE,
                -1
            )

            if (occurrenceMinute !in 0..1439) return

            ReminderDebugLogger.d("receiver fired id=$reminderId minute=$occurrenceMinute")
            FittoNotificationChannels.ensureCreated(context)

            ReminderAlarmScheduler.handleReminderTriggered(
                context = context.applicationContext,
                reminderId = reminderId,
                occurrenceMinute = occurrenceMinute,
                source = "alarm_receiver"
            )
        } finally {
            pendingResult.finish()
        }
    }
}
