package com.example.fitto.feature_home.presentation.profile.reminders.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationChannels
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderDebugLogger

class ReminderBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val reminderId = inputData.getString(ReminderAlarmScheduler.EXTRA_REMINDER_ID)
            ?: return Result.success()
        val occurrenceMinute = inputData.getInt(
            ReminderAlarmScheduler.EXTRA_OCCURRENCE_MINUTE,
            -1
        )

        if (occurrenceMinute !in 0..1439) return Result.success()

        ReminderDebugLogger.d("worker fired id=$reminderId minute=$occurrenceMinute")

        FittoNotificationChannels.ensureCreated(applicationContext)

        ReminderAlarmScheduler.handleReminderTriggered(
            context = applicationContext,
            reminderId = reminderId,
            occurrenceMinute = occurrenceMinute,
            source = "workmanager"
        )

        return Result.success()
    }
}
