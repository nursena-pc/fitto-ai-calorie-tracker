package com.example.fitto.feature_daily.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard

class FastingSessionStore(
    context: Context
) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun startFasting(
        planId: String,
        fastingHours: Int,
        startedAtMillis: Long = System.currentTimeMillis()
    ) {
        val endsAtMillis = startedAtMillis + fastingHours * HOUR_MILLIS

        prefs.edit()
            .putString(KEY_PLAN_ID, planId)
            .putLong(KEY_STARTED_AT, startedAtMillis)
            .putLong(KEY_ENDS_AT, endsAtMillis)
            .putInt(KEY_FASTING_HOURS, fastingHours)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getSession(): FastingSession? {
        val planId = prefs.getString(KEY_PLAN_ID, null) ?: return null
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val endsAt = prefs.getLong(KEY_ENDS_AT, 0L)
        val hours = prefs.getInt(KEY_FASTING_HOURS, 0)

        if (startedAt <= 0L || endsAt <= 0L || hours <= 0) return null

        return FastingSession(
            planId = planId,
            startedAtMillis = startedAt,
            endsAtMillis = endsAt,
            fastingHours = hours
        )
    }

    fun getRemainingMillis(planId: String): Long {
        val session = getSession() ?: return 0L
        if (session.planId != planId) return 0L

        return (session.endsAtMillis - System.currentTimeMillis())
            .coerceAtLeast(0L)
    }

    data class FastingSession(
        val planId: String,
        val startedAtMillis: Long,
        val endsAtMillis: Long,
        val fastingHours: Int
    )

    companion object {
        private const val PREF_NAME = "fitto_fasting_session"
        private const val KEY_PLAN_ID = "plan_id"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_ENDS_AT = "ends_at"
        private const val KEY_FASTING_HOURS = "fasting_hours"
        private const val HOUR_MILLIS = 60L * 60L * 1000L
    }
}