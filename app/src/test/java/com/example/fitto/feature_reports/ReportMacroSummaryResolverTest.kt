package com.example.fitto.feature_reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportMacroSummaryResolverTest {

    @Test
    fun resolve_usesLastDayAsSelectedDay() {
        val state = ReportMacroSummaryResolver.resolve(
            listOf(
                reportDay(protein = 10.0, carbs = 20.0, fat = 30.0),
                reportDay(protein = 40.0, carbs = 50.0, fat = 0.0)
            )
        )

        assertEquals(40.0, state.protein, 0.0)
        assertEquals(50.0, state.carbs, 0.0)
        assertEquals(0.0, state.fat, 0.0)
        assertEquals(90.0, state.total, 0.0)
        assertTrue(state.hasData)
        assertEquals(
            listOf(
                ReportMacroSummaryResolver.MacroType.PROTEIN,
                ReportMacroSummaryResolver.MacroType.CARBS
            ),
            state.chartEntries.map { it.type }
        )
    }

    @Test
    fun resolve_withoutMacros_returnsEmptyState() {
        val state = ReportMacroSummaryResolver.resolve(emptyList())

        assertEquals(0.0, state.total, 0.0)
        assertFalse(state.hasData)
        assertEquals(emptyList<ReportMacroSummaryResolver.MacroEntry>(), state.chartEntries)
    }

    private fun reportDay(
        protein: Double,
        carbs: Double,
        fat: Double
    ): ReportDay {
        return ReportDay(
            date = LocalDate.of(2026, 6, 25),
            label = "Thu",
            calories = 0,
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }
}
