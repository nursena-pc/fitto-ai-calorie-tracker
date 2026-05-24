package com.example.fitto.feature_home.presentation.profile.health

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeightTrend {
    UP,
    DOWN,
    SAME
}

data class WeightChartPoint(
    val timestamp: Long,
    val label: String,
    val weightKg: Double
)

data class WeightHistoryItem(
    val id: String,
    val createdAt: Long,
    val dateText: String,
    val weightKg: Double,
    val trend: WeightTrend = WeightTrend.SAME
)

data class ProfileHealthData(
    val initialWeightKg: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val targetWeightKg: Double = 0.0,
    val heightCm: Int = 0,
    val age: Int = 0,
    val genderKey: String = "",
    val goalTypeKey: String = "",
    val activityLevelKey: String = "",
    val bmr: Int = 0,
    val maintenanceCalories: Int = 0,
    val dailyTargetCalories: Int = 0,
    val weeklyWeightChangeKg: Double = 0.0,
    val estimatedWeeks: Int? = null,
    val waterTargetLiter: Double = 0.0,
    val proteinGoalGrams: Int = 0,
    val carbGoalGrams: Int = 0,
    val fatGoalGrams: Int = 0,
    val updatedAtMillis: Long = 0L,
    val lastWeighDateText: String = "",
    val chartPoints: List<WeightChartPoint> = emptyList(),
    val history: List<WeightHistoryItem> = emptyList()
) {
    val hasMainData: Boolean
        get() = currentWeightKg > 0.0 || targetWeightKg > 0.0 || dailyTargetCalories > 0

    val hasWeightData: Boolean
        get() = currentWeightKg > 0.0 || targetWeightKg > 0.0 || initialWeightKg > 0.0

    val hasWeightGoal: Boolean
        get() = currentWeightKg > 0.0 && targetWeightKg > 0.0

    val weightDifferenceKg: Double
        get() = if (hasWeightGoal) {
            abs(currentWeightKg - targetWeightKg)
        } else {
            0.0
        }

    val weightGoalProgressPercent: Int
        get() {
            if (!hasWeightGoal) return 0

            val baseDistance = if (initialWeightKg > 0.0) {
                abs(initialWeightKg - targetWeightKg).coerceAtLeast(0.1)
            } else {
                maxOf(10.0, abs(currentWeightKg - targetWeightKg))
            }

            val currentDistance = abs(currentWeightKg - targetWeightKg)

            return ((baseDistance - currentDistance) / baseDistance * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        }

    val lostSoFarKg: Double
        get() = if (initialWeightKg > 0.0 && currentWeightKg > 0.0) {
            (initialWeightKg - currentWeightKg).coerceAtLeast(0.0)
        } else {
            0.0
        }

    val remainingKg: Double
        get() = if (targetWeightKg > 0.0 && currentWeightKg > 0.0) {
            abs(currentWeightKg - targetWeightKg)
        } else {
            0.0
        }

    companion object {
        val Empty = ProfileHealthData()
    }
}

fun formatWeightKg(value: Double): String {
    if (value <= 0.0) return ""

    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 3
        minimumFractionDigits = 0
        isGroupingUsed = false
    }

    return formatter.format(value)
}

fun formatSignedWeightKg(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 3
        minimumFractionDigits = 0
        isGroupingUsed = false
    }
    val prefix = if (value > 0.0) "+" else ""
    return prefix + formatter.format(value)
}
