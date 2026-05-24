package com.example.fitto.feature_home.presentation.profile.reminders.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_home.presentation.profile.ReminderPreferenceStore
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileRemindersActivity
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.absoluteValue

object ReminderAlarmScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.example.fitto.action.TRIGGER_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_OCCURRENCE_MINUTE = "extra_occurrence_minute"

    private const val PREF_NAME = "fitto_scheduled_reminder_alarms"
    private const val KEY_REQUEST_CODES = "request_codes_by_reminder"
    private const val MIN_TRIGGER_DELAY_MS = 5_000L

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun scheduleAll(context: Context) {
        val appContext = context.applicationContext
        cancelAll(appContext)

        if (!canScheduleReminders(appContext)) {
            ReminderDebugLogger.d("scheduleAll skipped: reminders disabled")
            return
        }

        ReminderPreferenceStore.getReminders(appContext)
            .filter { it.isEnabled }
            .forEach { item ->
                scheduleReminder(
                    context = appContext,
                    item = item,
                    cancelPrevious = false
                )
            }
    }

    fun scheduleReminder(context: Context, item: ProfileReminderItem) {
        scheduleReminder(
            context = context,
            item = item,
            cancelPrevious = true
        )
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val appContext = context.applicationContext
        val stored = readStoredRequestCodes(appContext)
        val codes = stored.optJSONArray(reminderId)

        if (codes != null) {
            for (index in 0 until codes.length()) {
                cancelPendingIntent(
                    context = appContext,
                    requestCode = codes.optInt(index)
                )
            }
        }

        stored.remove(reminderId)
        writeStoredRequestCodes(appContext, stored)
        ReminderBackupScheduler.cancel(appContext, reminderId)
        ReminderDebugLogger.d("alarm cancelled id=$reminderId")
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val stored = readStoredRequestCodes(appContext)
        val keys = stored.keys()

        while (keys.hasNext()) {
            val reminderId = keys.next()
            val codes = stored.optJSONArray(reminderId) ?: continue

            for (index in 0 until codes.length()) {
                cancelPendingIntent(
                    context = appContext,
                    requestCode = codes.optInt(index)
                )
            }
        }

        preferences(appContext)
            .edit()
            .remove(KEY_REQUEST_CODES)
            .apply()

        ReminderBackupScheduler.cancelAll(appContext)
        ReminderDebugLogger.d("all alarms cancelled")
    }

    fun handleReminderTriggered(
        context: Context,
        reminderId: String,
        occurrenceMinute: Int,
        source: String = "alarm"
    ) {
        val appContext = context.applicationContext
        ReminderDebugLogger.d("trigger received source=$source id=$reminderId minute=$occurrenceMinute")

        val item = ReminderPreferenceStore.getReminders(appContext)
            .firstOrNull { it.id == reminderId }
            ?: run {
                ReminderDebugLogger.w("trigger ignored: item not found id=$reminderId")
                return
            }

        if (!item.isEnabled) {
            ReminderDebugLogger.d("trigger ignored: disabled id=$reminderId")
            return
        }

        if (!isTodayAllowed(item)) {
            ReminderDebugLogger.d("trigger skipped: day not allowed id=$reminderId")
            scheduleReminder(appContext, item)
            return
        }

        if (!ReminderDeliveryGuard.shouldDeliver(appContext, reminderId, occurrenceMinute)) {
            return
        }

        com.example.fitto.feature_home.presentation.notifications.FittoNotificationPublisher
            .showReminderNotification(
                context = appContext,
                item = item,
                occurrenceMinute = occurrenceMinute
            )

        if (item.repeatMode == ReminderRepeatMode.ONCE) {
            ReminderPreferenceStore.setEnabled(
                context = appContext,
                id = item.id,
                enabled = false
            )
            cancelReminder(appContext, item.id)
        } else {
            scheduleReminder(appContext, item)
        }
    }

    private fun scheduleReminder(
        context: Context,
        item: ProfileReminderItem,
        cancelPrevious: Boolean
    ) {
        val appContext = context.applicationContext

        if (cancelPrevious) {
            cancelReminder(appContext, item.id)
        }

        if (!item.isEnabled || !canScheduleReminders(appContext)) {
            ReminderDebugLogger.d("schedule skipped id=${item.id} enabled=${item.isEnabled}")
            return
        }

        val nextTrigger = findNextTrigger(item) ?: run {
            ReminderDebugLogger.w("schedule skipped: no next trigger id=${item.id}")
            return
        }

        val requestCode = requestCodeFor(
            reminderId = item.id,
            occurrenceMinute = nextTrigger.occurrenceMinute
        )

        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, item.id)
            putExtra(EXTRA_OCCURRENCE_MINUTE, nextTrigger.occurrenceMinute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: run {
            ReminderDebugLogger.w("schedule failed: AlarmManager null id=${item.id}")
            return
        }

        scheduleAlarm(
            context = appContext,
            alarmManager = alarmManager,
            triggerAtMillis = nextTrigger.triggerAtMillis,
            pendingIntent = pendingIntent,
            reminderId = item.id
        )

        ReminderBackupScheduler.schedule(
            context = appContext,
            reminderId = item.id,
            occurrenceMinute = nextTrigger.occurrenceMinute,
            triggerAtMillis = nextTrigger.triggerAtMillis
        )

        storeRequestCodes(
            context = appContext,
            reminderId = item.id,
            requestCodes = listOf(requestCode)
        )

        ReminderDebugLogger.d(
            "alarm scheduled id=${item.id} minute=${nextTrigger.occurrenceMinute} at=${nextTrigger.triggerAtMillis}"
        )
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        reminderId: String
    ) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(
                        triggerAtMillis,
                        reminderScreenPendingIntent(context, reminderId)
                    ),
                    pendingIntent
                )
                ReminderDebugLogger.d("alarmClock exact alarm used id=$reminderId")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                ReminderDebugLogger.w(
                    "exact alarm permission missing; inexact idle alarm used id=$reminderId"
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(
                        triggerAtMillis,
                        reminderScreenPendingIntent(context, reminderId)
                    ),
                    pendingIntent
                )
                ReminderDebugLogger.d("legacy alarmClock alarm used id=$reminderId")
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                ReminderDebugLogger.d("pre-m exact alarm used id=$reminderId")
            }
        }
    }

    private fun reminderScreenPendingIntent(
        context: Context,
        reminderId: String
    ): PendingIntent {
        val intent = Intent(context.applicationContext, ProfileRemindersActivity::class.java)
        return PendingIntent.getActivity(
            context.applicationContext,
            "show_$reminderId".hashCode().absoluteValue,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canScheduleReminders(context: Context): Boolean {
        return ProfilePreferenceStore.areNotificationsAllowed(context) &&
                ProfilePreferenceStore.areReminderNotificationsEnabled(context)
    }

    private fun findNextTrigger(item: ProfileReminderItem): NextReminderTrigger? {
        val now = Calendar.getInstance()
        val minAllowedMillis = now.timeInMillis + MIN_TRIGGER_DELAY_MS
        val occurrenceMinutes = occurrenceMinutesFor(item)

        for (dayOffset in 0..14) {
            val dayCalendar = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (!isDayAllowed(item, dayCalendar, dayOffset)) continue

            occurrenceMinutes.forEach { minuteOfDay ->
                val triggerCalendar = dayCalendar.clone() as Calendar
                triggerCalendar.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                triggerCalendar.set(Calendar.MINUTE, minuteOfDay % 60)

                if (triggerCalendar.timeInMillis >= minAllowedMillis) {
                    return NextReminderTrigger(
                        triggerAtMillis = triggerCalendar.timeInMillis,
                        occurrenceMinute = minuteOfDay
                    )
                }
            }
        }

        return null
    }

    private fun occurrenceMinutesFor(item: ProfileReminderItem): List<Int> {
        if (!item.isWaterInterval) return listOf(item.minuteOfDay.coerceIn(0, 23 * 60 + 59))

        val minutes = mutableListOf<Int>()
        var current = item.waterStartMinute.coerceIn(0, 23 * 60 + 59)
        val end = item.waterEndMinute.coerceIn(0, 23 * 60 + 59)
        val interval = item.waterIntervalMinute.coerceAtLeast(30)

        while (current <= end) {
            minutes.add(current)
            current += interval
        }

        return minutes.ifEmpty { listOf(item.minuteOfDay.coerceIn(0, 23 * 60 + 59)) }
    }

    private fun isTodayAllowed(item: ProfileReminderItem): Boolean {
        return isDayAllowed(
            item = item,
            calendar = Calendar.getInstance(),
            dayOffset = 0
        )
    }

    private fun isDayAllowed(
        item: ProfileReminderItem,
        calendar: Calendar,
        dayOffset: Int
    ): Boolean {
        if (item.repeatMode == ReminderRepeatMode.ONCE && dayOffset > 1) return false

        val appDay = calendar.toFittoDayNumber()
        val selectedDays = item.selectedDays
            .filter { it in 1..7 }
            .toSet()

        return when (item.repeatMode) {
            ReminderRepeatMode.ONCE -> true
            ReminderRepeatMode.WEEKDAYS -> appDay in 1..5
            ReminderRepeatMode.WEEKENDS -> appDay in 6..7
            ReminderRepeatMode.WEEKLY -> {
                val allowedDays = selectedDays.ifEmpty { setOf(Calendar.getInstance().toFittoDayNumber()) }
                appDay in allowedDays
            }
            ReminderRepeatMode.DAILY -> {
                val allowedDays = selectedDays.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
                appDay in allowedDays
            }
        }
    }

    private fun Calendar.toFittoDayNumber(): Int {
        return when (get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    private fun requestCodeFor(
        reminderId: String,
        occurrenceMinute: Int
    ): Int {
        return "${reminderId}_$occurrenceMinute".hashCode().absoluteValue
    }

    private fun cancelPendingIntent(
        context: Context,
        requestCode: Int
    ) {
        val intent = Intent(context.applicationContext, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val alarmManager = context.applicationContext.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun storeRequestCodes(
        context: Context,
        reminderId: String,
        requestCodes: List<Int>
    ) {
        val stored = readStoredRequestCodes(context)
        val array = JSONArray()
        requestCodes.forEach { code -> array.put(code) }
        stored.put(reminderId, array)
        writeStoredRequestCodes(context, stored)
    }

    private fun readStoredRequestCodes(context: Context): JSONObject {
        val raw = preferences(context).getString(KEY_REQUEST_CODES, null)
        return runCatching { JSONObject(raw ?: "{}") }.getOrDefault(JSONObject())
    }

    private fun writeStoredRequestCodes(context: Context, value: JSONObject) {
        preferences(context)
            .edit()
            .putString(KEY_REQUEST_CODES, value.toString())
            .apply()
    }

    private data class NextReminderTrigger(
        val triggerAtMillis: Long,
        val occurrenceMinute: Int
    )
}
