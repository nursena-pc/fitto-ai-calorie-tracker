package com.example.fitto.feature_sleep.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import org.json.JSONObject
import java.time.LocalDate

class SleepLogStore(context: Context) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun saveLog(log: SleepLogLocalModel) {
        val json = JSONObject().apply {
            put("date", log.date)
            put("bedtimeHour", log.bedtimeHour)
            put("bedtimeMinute", log.bedtimeMinute)
            put("wakeHour", log.wakeHour)
            put("wakeMinute", log.wakeMinute)
            put("durationMinutes", log.durationMinutes)
            put("caloriesBurned", log.caloriesBurned)
        }
        prefs.edit().putString(log.date, json.toString()).apply()
    }

    fun getLogForDate(date: String): SleepLogLocalModel? {
        val raw = prefs.getString(date, null) ?: return null
        val json = JSONObject(raw)

        return SleepLogLocalModel(
            date = json.optString("date"),
            bedtimeHour = json.optInt("bedtimeHour"),
            bedtimeMinute = json.optInt("bedtimeMinute"),
            wakeHour = json.optInt("wakeHour"),
            wakeMinute = json.optInt("wakeMinute"),
            durationMinutes = json.optInt("durationMinutes"),
            caloriesBurned = json.optInt("caloriesBurned")
        )
    }
    fun getLogsForLastDays(endDate: LocalDate = LocalDate.now(), dayCount: Int = 7): List<SleepLogLocalModel> {
        return (dayCount - 1 downTo 0).map { offset ->
            val date = endDate.minusDays(offset.toLong()).toString()
            getLogForDate(date) ?: SleepLogLocalModel(
                date = date,
                bedtimeHour = 0,
                bedtimeMinute = 0,
                wakeHour = 0,
                wakeMinute = 0,
                durationMinutes = 0,
                caloriesBurned = 0
            )
        }
    }


    fun deleteLog(date: String) {
        prefs.edit().remove(date).apply()
    }


    companion object {
        private const val PREF_NAME = "fitto_sleep_logs"
    }
}
