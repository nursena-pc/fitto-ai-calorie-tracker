package com.example.fitto.feature_home.presentation.profile.reminders.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderDebugLogger

class ReminderTaskRemovedService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ReminderDebugLogger.d("task service started")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        ReminderDebugLogger.d("task removed from recents; rescheduling reminders")
        ReminderAlarmScheduler.scheduleAll(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        fun ensureStarted(context: Context) {
            runCatching {
                context.applicationContext.startService(
                    Intent(context.applicationContext, ReminderTaskRemovedService::class.java)
                )
            }.onFailure { throwable ->
                ReminderDebugLogger.w("task service could not start", throwable)
            }
        }
    }
}
