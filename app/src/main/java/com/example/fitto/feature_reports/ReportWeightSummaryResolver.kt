package com.example.fitto.feature_reports

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint

object ReportWeightSummaryResolver {

    data class State(
        val allPoints: List<WeightChartPoint>,
        val visiblePoints: List<WeightChartPoint>,
        val firstWeight: Double,
        val currentWeight: Double,
        val targetWeight: Double,
        val axisMin: Float,
        val axisMax: Float
    ) {
        val hasData: Boolean = allPoints.isNotEmpty()
    }

    fun resolve(
        healthData: ProfileHealthData,
        visibleCount: Int,
        axisPaddingKg: Double
    ): State {
        val allPoints = healthData.chartPoints
            .filter { point -> point.weightKg > 0.0 }
            .sortedBy { point -> point.timestamp }

        if (allPoints.isEmpty()) {
            return State(
                allPoints = emptyList(),
                visiblePoints = emptyList(),
                firstWeight = 0.0,
                currentWeight = 0.0,
                targetWeight = healthData.targetWeightKg,
                axisMin = 0f,
                axisMax = 0f
            )
        }

        val safeVisibleCount = visibleCount.coerceAtLeast(1)
        val visiblePoints = allPoints.takeLast(safeVisibleCount)
        val currentWeight = allPoints.last().weightKg
        val minWeight = visiblePoints.minOfOrNull { point -> point.weightKg } ?: currentWeight
        val maxWeight = visiblePoints.maxOfOrNull { point -> point.weightKg } ?: currentWeight

        return State(
            allPoints = allPoints,
            visiblePoints = visiblePoints,
            firstWeight = allPoints.first().weightKg,
            currentWeight = currentWeight,
            targetWeight = healthData.targetWeightKg,
            axisMin = (minWeight - axisPaddingKg).coerceAtLeast(0.0).toFloat(),
            axisMax = (maxWeight + axisPaddingKg).toFloat()
        )
    }
}
