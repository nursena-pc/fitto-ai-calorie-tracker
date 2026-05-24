package com.example.fitto.feature_home.presentation.profile.reminders.alarm

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.fitto.feature_home.presentation.profile.reminders.worker.ReminderBackupWorker
import java.util.concurrent.TimeUnit

object ReminderBackupScheduler {

    const val TAG_REMINDER_BACKUP = "fitto_reminder_backup"

    fun schedule(
        context: Context,
        reminderId: String,
        occurrenceMinute: Int,
        triggerAtMillis: Long
    ) {
        val appContext = context.applicationContext
        val delayMs = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val workName = workNameFor(reminderId)

        val request = OneTimeWorkRequestBuilder<ReminderBackupWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderAlarmScheduler.EXTRA_REMINDER_ID to reminderId,
                    ReminderAlarmScheduler.EXTRA_OCCURRENCE_MINUTE to occurrenceMinute
                )
            )
            .addTag(TAG_REMINDER_BACKUP)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)

        ReminderDebugLogger.d(
            "backup scheduled id=$reminderId minute=$occurrenceMinute delayMs=$delayMs work=$workName"
        )
    }

    fun cancel(
        context: Context,
        reminderId: String
    ) {
        val appContext = context.applicationContext
        WorkManager.getInstance(appContext).cancelUniqueWork(workNameFor(reminderId))
        ReminderDebugLogger.d("backup cancelled id=$reminderId")
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        WorkManager.getInstance(appContext).cancelAllWorkByTag(TAG_REMINDER_BACKUP)
        ReminderDebugLogger.d("all backup work cancelled")
    }

    private fun workNameFor(reminderId: String): String {
        return "fitto_reminder_backup_$reminderId"
    }
}
