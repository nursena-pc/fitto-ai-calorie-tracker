package com.example.fitto.feature_health

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import java.time.LocalDate

class FittoStepCounterStore(
    context: Context
) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getCachedSteps(date: LocalDate): Long {
        return prefs.getLong(key(KEY_CACHED_STEPS, date), 0L)
            .coerceAtLeast(0L)
    }

    fun updateFromSensorTotal(
        date: LocalDate,
        sensorTotalSteps: Long
    ): Long {
        val safeSensorTotal = sensorTotalSteps.coerceAtLeast(0L)

        val baselineKey = key(KEY_BASELINE_TOTAL, date)
        val accumulatedKey = key(KEY_ACCUMULATED_BEFORE_BASELINE, date)
        val lastTotalKey = key(KEY_LAST_SENSOR_TOTAL, date)
        val cachedKey = key(KEY_CACHED_STEPS, date)

        val hasBaseline = prefs.contains(baselineKey)

        var baseline = prefs.getLong(baselineKey, safeSensorTotal)
        var accumulated = prefs.getLong(accumulatedKey, 0L).coerceAtLeast(0L)
        val lastTotal = prefs.getLong(lastTotalKey, safeSensorTotal)

        if (!hasBaseline) {
            baseline = safeSensorTotal
            accumulated = 0L
        } else if (safeSensorTotal < lastTotal || safeSensorTotal < baseline) {
            // Telefon yeniden başladıysa TYPE_STEP_COUNTER değeri düşer.
            // O güne ait önceki farkı accumulated içine alıp yeni baseline başlatıyoruz.
            accumulated += (lastTotal - baseline).coerceAtLeast(0L)
            baseline = safeSensorTotal
        }

        val currentSteps = (
                accumulated + (safeSensorTotal - baseline).coerceAtLeast(0L)
                ).coerceAtLeast(0L)

        prefs.edit()
            .putLong(baselineKey, baseline)
            .putLong(accumulatedKey, accumulated)
            .putLong(lastTotalKey, safeSensorTotal)
            .putLong(cachedKey, currentSteps)
            .apply()

        return currentSteps
    }

    private fun key(prefix: String, date: LocalDate): String {
        return "${prefix}_${date}"
    }

    companion object {
        private const val PREF_NAME = "fitto_step_counter"
        private const val KEY_BASELINE_TOTAL = "baseline_total"
        private const val KEY_ACCUMULATED_BEFORE_BASELINE = "accumulated_before_baseline"
        private const val KEY_LAST_SENSOR_TOTAL = "last_sensor_total"
        private const val KEY_CACHED_STEPS = "cached_steps"
    }
}