package com.example.fitto.feature_reports

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportWeightSummaryResolverTest {

    @Test
    fun resolve_filtersSortsAndLimitsVisiblePoints() {
        val state = ReportWeightSummaryResolver.resolve(
            healthData = ProfileHealthData(
                targetWeightKg = 65.0,
                chartPoints = listOf(
                    point(timestamp = 3L, weight = 70.0),
                    point(timestamp = 1L, weight = 72.0),
                    point(timestamp = 2L, weight = 0.0),
                    point(timestamp = 4L, weight = 68.0)
                )
            ),
            visibleCount = 2,
            axisPaddingKg = 2.0
        )

        assertTrue(state.hasData)
        assertEquals(listOf(72.0, 70.0, 68.0), state.allPoints.map { it.weightKg })
        assertEquals(listOf(70.0, 68.0), state.visiblePoints.map { it.weightKg })
        assertEquals(72.0, state.firstWeight, 0.0)
        assertEquals(68.0, state.currentWeight, 0.0)
        assertEquals(65.0, state.targetWeight, 0.0)
        assertEquals(66.0f, state.axisMin, 0.0f)
        assertEquals(72.0f, state.axisMax, 0.0f)
    }

    @Test
    fun resolve_withoutPointsKeepsTargetButMarksNoData() {
        val state = ReportWeightSummaryResolver.resolve(
            healthData = ProfileHealthData(
                targetWeightKg = 65.0,
                chartPoints = emptyList()
            ),
            visibleCount = 4,
            axisPaddingKg = 2.0
        )

        assertFalse(state.hasData)
        assertEquals(65.0, state.targetWeight, 0.0)
        assertEquals(emptyList<WeightChartPoint>(), state.visiblePoints)
    }

    private fun point(
        timestamp: Long,
        weight: Double
    ): WeightChartPoint {
        return WeightChartPoint(
            timestamp = timestamp,
            label = "Day $timestamp",
            weightKg = weight
        )
    }
}
