package com.example.fitto.feature_reports

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportsRenderStateResolverTest {

    @Test
    fun resolve_buildsAllPageStatesFromSingleDataSnapshot() {
        val dataState = ReportsDataState(
            selectedDate = LocalDate.of(2026, 7, 4),
            nutritionDays = listOf(
                reportDay(
                    date = LocalDate.of(2026, 7, 3),
                    calories = 1600,
                    protein = 90.0,
                    carbs = 180.0,
                    fat = 55.0
                ),
                reportDay(
                    date = LocalDate.of(2026, 7, 4),
                    calories = 2000,
                    protein = 110.0,
                    carbs = 210.0,
                    fat = 70.0
                )
            ),
            healthData = ProfileHealthData(
                dailyTargetCalories = 2200,
                targetWeightKg = 70.0,
                chartPoints = listOf(
                    weightPoint(timestamp = 1L, label = "1 Tem", weightKg = 76.0),
                    weightPoint(timestamp = 2L, label = "2 Tem", weightKg = 75.0),
                    weightPoint(timestamp = 3L, label = "3 Tem", weightKg = 74.0)
                )
            )
        )

        val state = ReportsRenderStateResolver.resolve(
            dataState = dataState,
            config = ReportsRenderConfig(
                defaultTargetCalories = 1800,
                weightVisiblePointCount = 2,
                weightAxisPaddingKg = 2.0
            )
        )

        assertEquals(2200, state.calorie.targetCalories)
        assertEquals(2000, state.calorie.todayCalories)
        assertEquals(1800, state.calorie.averageCalories)
        assertEquals(110.0, state.macro.protein, 0.0)
        assertEquals(210.0, state.macro.carbs, 0.0)
        assertEquals(70.0, state.macro.fat, 0.0)
        assertEquals(listOf("2 Tem", "3 Tem"), state.weight.visiblePoints.map { it.label })
        assertEquals(72f, state.weight.axisMin, 0f)
        assertEquals(77f, state.weight.axisMax, 0f)
    }

    @Test
    fun resolve_usesConfiguredFallbacksWhenProfileAndWeightDataAreEmpty() {
        val state = ReportsRenderStateResolver.resolve(
            dataState = ReportsDataState(
                selectedDate = LocalDate.of(2026, 7, 4),
                nutritionDays = emptyList(),
                healthData = ProfileHealthData.Empty
            ),
            config = ReportsRenderConfig(
                defaultTargetCalories = 1900,
                weightVisiblePointCount = 7,
                weightAxisPaddingKg = 3.0
            )
        )

        assertEquals(1900, state.calorie.targetCalories)
        assertEquals(0, state.calorie.todayCalories)
        assertEquals(0.0, state.macro.total, 0.0)
        assertEquals(false, state.weight.hasData)
        assertEquals(0f, state.weight.axisMin, 0f)
        assertEquals(0f, state.weight.axisMax, 0f)
    }

    private fun reportDay(
        date: LocalDate,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double
    ): ReportDay {
        return ReportDay(
            date = date,
            label = date.dayOfWeek.name,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }

    private fun weightPoint(
        timestamp: Long,
        label: String,
        weightKg: Double
    ): WeightChartPoint {
        return WeightChartPoint(
            timestamp = timestamp,
            label = label,
            weightKg = weightKg
        )
    }
}
