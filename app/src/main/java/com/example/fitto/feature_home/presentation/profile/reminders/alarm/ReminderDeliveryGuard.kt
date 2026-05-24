package com.example.fitto.feature_home.presentation.profile.reminders.alarm

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import java.util.Calendar

object ReminderDeliveryGuard {

    private const val PREF_NAME = "fitto_reminder_delivery_guard"
    private const val DUPLICATE_WINDOW_MS = 30 * 60 * 1_000L

    private fun preferences(context: Context) =
        UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun shouldDeliver(
        context: Context,
        reminderId: String,
        occurrenceMinute: Int,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val key = "${reminderId}_${occurrenceMinute}_${dayKey()}"
        val prefs = preferences(context)
        val lastDeliveredAt = prefs.getLong(key, 0L)

        if (lastDeliveredAt > 0L && now - lastDeliveredAt in 0 until DUPLICATE_WINDOW_MS) {
            ReminderDebugLogger.d(
                "duplicate blocked id=$reminderId minute=$occurrenceMinute last=$lastDeliveredAt now=$now"
            )
            return false
        }

        prefs.edit()
            .putLong(key, now)
            .apply()

        return true
    }

    private fun dayKey(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}_${calendar.get(Calendar.DAY_OF_YEAR)}"
    }
}
