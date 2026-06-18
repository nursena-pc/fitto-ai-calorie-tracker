package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.data.WaterTrackingStore
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

object DailyMetricCalculator {

    fun calculateStepCalories(
        steps: Long,
        weightKg: Double
    ): Int {
        val kcalPerStep = when {
            weightKg < 55 -> 0.032
            weightKg < 70 -> 0.038
            weightKg < 85 -> 0.044
            else -> 0.050
        }

        return (steps * kcalPerStep).roundToInt().coerceAtLeast(0)
    }

    fun calculateSleepDurationMinutes(
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeHour: Int,
        wakeMinute: Int
    ): Int {
        val today = LocalDate.now()
        val bedtime = LocalDateTime.of(
            today,
            LocalTime.of(bedtimeHour, bedtimeMinute)
        )

        var wake = LocalDateTime.of(
            today,
            LocalTime.of(wakeHour, wakeMinute)
        )

        if (!wake.isAfter(bedtime)) {
            wake = wake.plusDays(1)
        }

        return Duration.between(bedtime, wake).toMinutes().toInt().coerceAtLeast(0)
    }

    fun calculateSleepCalories(
        durationMinutes: Int,
        weightKg: Double
    ): Int {
        val hours = durationMinutes / 60.0
        val sleepMet = 0.95
        return (sleepMet * weightKg * hours).roundToInt().coerceAtLeast(0)
    }

    fun roundUpToBottleSize(valueMl: Int): Int {
        val size = WaterTrackingStore.BOTTLE_SIZE_ML
        return ((valueMl + size - 1) / size) * size
    }

    fun normalizeWaterToBottleSize(valueMl: Int): Int {
        val bottleSizeMl = WaterTrackingStore.BOTTLE_SIZE_ML
        return (valueMl.coerceAtLeast(0) / bottleSizeMl) * bottleSizeMl
    }

    fun progressPercent(
        current: Double,
        target: Double
    ): Int {
        if (target <= 0.0) return 0

        return ((current / target) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }
}
