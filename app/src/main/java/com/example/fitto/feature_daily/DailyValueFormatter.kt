package com.example.fitto.feature_daily

import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object DailyValueFormatter {

    fun formatHourMinute(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        return if (Locale.getDefault().language.equals("tr", ignoreCase = true)) {
            "${hours}sa ${minutes}dk"
        } else {
            "${hours}h ${minutes}m"
        }
    }

    fun formatWeightValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    fun formatRemainingTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun formatStepCount(steps: Long): String {
        return NumberFormat
            .getIntegerInstance(Locale.getDefault())
            .format(steps.coerceAtLeast(0L))
    }

    fun formatGram(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
