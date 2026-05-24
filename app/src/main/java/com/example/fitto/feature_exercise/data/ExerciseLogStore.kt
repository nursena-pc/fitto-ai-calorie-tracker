package com.example.fitto.feature_exercise.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ExerciseLogStore(
    context: Context
) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun addLog(
        entryDate: String,
        exerciseId: String,
        exerciseTitle: String,
        categoryName: String,
        durationMinutes: Int,
        intensityName: String,
        metValue: Double,
        calories: Int
    ): ExerciseLogLocalModel {
        val now = System.currentTimeMillis()

        val log = ExerciseLogLocalModel(
            id = UUID.randomUUID().toString(),
            entryDate = entryDate,
            exerciseId = exerciseId,
            exerciseTitle = exerciseTitle,
            categoryName = categoryName,
            durationMinutes = durationMinutes.coerceAtLeast(1),
            intensityName = intensityName,
            metValue = metValue,
            calories = calories.coerceAtLeast(0),
            createdAt = now
        )

        val updatedLogs = readLogs().toMutableList()
        updatedLogs.add(log)
        writeLogs(updatedLogs)

        return log
    }

    fun getLogsForDate(entryDate: String): List<ExerciseLogLocalModel> {
        return readLogs()
            .filter { it.entryDate == entryDate }
            .sortedByDescending { it.createdAt }
    }

    fun getSummaryForDate(entryDate: String): ExerciseDailySummary {
        val logs = getLogsForDate(entryDate)

        return ExerciseDailySummary(
            totalCalories = logs.sumOf { it.calories },
            totalDurationMinutes = logs.sumOf { it.durationMinutes },
            logCount = logs.size
        )
    }

    fun deleteLog(logId: String) {
        val updatedLogs = readLogs()
            .filterNot { it.id == logId }

        writeLogs(updatedLogs)
    }

    private fun readLogs(): List<ExerciseLogLocalModel> {
        val rawJson = prefs.getString(KEY_LOGS, "[]").orEmpty()
        val array = runCatching { JSONArray(rawJson) }.getOrElse { JSONArray() }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(item.toExerciseLog())
            }
        }
    }
    fun updateLog(
        logId: String,
        exerciseId: String,
        exerciseTitle: String,
        categoryName: String,
        durationMinutes: Int,
        intensityName: String,
        metValue: Double,
        calories: Int
    ) {
        val updatedLogs = readLogs().map { log ->
            if (log.id == logId) {
                log.copy(
                    exerciseId = exerciseId,
                    exerciseTitle = exerciseTitle,
                    categoryName = categoryName,
                    durationMinutes = durationMinutes.coerceAtLeast(1),
                    intensityName = intensityName,
                    metValue = metValue,
                    calories = calories.coerceAtLeast(0)
                )
            } else {
                log
            }
        }

        writeLogs(updatedLogs)
    }
    private fun writeLogs(logs: List<ExerciseLogLocalModel>) {
        val array = JSONArray()

        logs.forEach { log ->
            array.put(log.toJson())
        }

        prefs.edit()
            .putString(KEY_LOGS, array.toString())
            .apply()
    }

    private fun ExerciseLogLocalModel.toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, id)
            put(KEY_ENTRY_DATE, entryDate)
            put(KEY_EXERCISE_ID, exerciseId)
            put(KEY_EXERCISE_TITLE, exerciseTitle)
            put(KEY_CATEGORY_NAME, categoryName)
            put(KEY_DURATION_MINUTES, durationMinutes)
            put(KEY_INTENSITY_NAME, intensityName)
            put(KEY_MET_VALUE, metValue)
            put(KEY_CALORIES, calories)
            put(KEY_CREATED_AT, createdAt)
        }
    }

    private fun JSONObject.toExerciseLog(): ExerciseLogLocalModel {
        return ExerciseLogLocalModel(
            id = optString(KEY_ID),
            entryDate = optString(KEY_ENTRY_DATE),
            exerciseId = optString(KEY_EXERCISE_ID),
            exerciseTitle = optString(KEY_EXERCISE_TITLE),
            categoryName = optString(KEY_CATEGORY_NAME),
            durationMinutes = optInt(KEY_DURATION_MINUTES, 0),
            intensityName = optString(KEY_INTENSITY_NAME),
            metValue = optDouble(KEY_MET_VALUE, 0.0),
            calories = optInt(KEY_CALORIES, 0),
            createdAt = optLong(KEY_CREATED_AT, 0L)
        )
    }

    companion object {
        private const val PREF_NAME = "fitto_exercise_logs"
        private const val KEY_LOGS = "exercise_logs"

        private const val KEY_ID = "id"
        private const val KEY_ENTRY_DATE = "entry_date"
        private const val KEY_EXERCISE_ID = "exercise_id"
        private const val KEY_EXERCISE_TITLE = "exercise_title"
        private const val KEY_CATEGORY_NAME = "category_name"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
        private const val KEY_INTENSITY_NAME = "intensity_name"
        private const val KEY_MET_VALUE = "met_value"
        private const val KEY_CALORIES = "calories"
        private const val KEY_CREATED_AT = "created_at"
    }
}