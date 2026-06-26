package com.example.fitto.feature_daily

import com.example.fitto.feature_sleep.data.SleepLogLocalModel

data class DailySleepTimeSelection(
    val bedtimeHour: Int,
    val bedtimeMinute: Int,
    val wakeHour: Int,
    val wakeMinute: Int
) {
    fun withBedtime(hour: Int, minute: Int): DailySleepTimeSelection {
        return copy(
            bedtimeHour = hour,
            bedtimeMinute = minute
        )
    }

    fun withWakeTime(hour: Int, minute: Int): DailySleepTimeSelection {
        return copy(
            wakeHour = hour,
            wakeMinute = minute
        )
    }

    fun durationMinutes(): Int {
        return DailyMetricCalculator.calculateSleepDurationMinutes(
            bedtimeHour = bedtimeHour,
            bedtimeMinute = bedtimeMinute,
            wakeHour = wakeHour,
            wakeMinute = wakeMinute
        )
    }

    fun calories(weightKg: Double): Int {
        return DailyMetricCalculator.calculateSleepCalories(
            durationMinutes = durationMinutes(),
            weightKg = weightKg
        )
    }

    fun toLog(date: String, weightKg: Double): SleepLogLocalModel {
        val durationMinutes = durationMinutes()
        val calories = DailyMetricCalculator.calculateSleepCalories(
            durationMinutes = durationMinutes,
            weightKg = weightKg
        )

        return SleepLogLocalModel(
            date = date,
            bedtimeHour = bedtimeHour,
            bedtimeMinute = bedtimeMinute,
            wakeHour = wakeHour,
            wakeMinute = wakeMinute,
            durationMinutes = durationMinutes,
            caloriesBurned = calories
        )
    }

    companion object {
        private const val DEFAULT_BEDTIME_HOUR = 23
        private const val DEFAULT_BEDTIME_MINUTE = 0
        private const val DEFAULT_WAKE_HOUR = 7
        private const val DEFAULT_WAKE_MINUTE = 0

        fun from(log: SleepLogLocalModel?): DailySleepTimeSelection {
            return log?.let {
                DailySleepTimeSelection(
                    bedtimeHour = it.bedtimeHour,
                    bedtimeMinute = it.bedtimeMinute,
                    wakeHour = it.wakeHour,
                    wakeMinute = it.wakeMinute
                )
            } ?: DailySleepTimeSelection(
                bedtimeHour = DEFAULT_BEDTIME_HOUR,
                bedtimeMinute = DEFAULT_BEDTIME_MINUTE,
                wakeHour = DEFAULT_WAKE_HOUR,
                wakeMinute = DEFAULT_WAKE_MINUTE
            )
        }
    }
}
