package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DailyDateSelectionActionTest {

    private val today = LocalDate.of(2026, 7, 2)
    private val action = DailyDateSelectionAction(todayProvider = { today })

    @Test
    fun `picker state uses utc start of day for selection and latest date`() {
        val selectedDate = LocalDate.of(2026, 6, 28)

        val state = action.pickerState(selectedDate)

        assertEquals(
            selectedDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            state.selectedUtcMillis
        )
        assertEquals(
            today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            state.latestUtcMillis
        )
    }

    @Test
    fun `utc millis converts back to the same calendar date`() {
        val date = LocalDate.of(2026, 1, 15)
        val millis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        assertEquals(date, action.dateFromUtcMillis(millis))
    }

    @Test
    fun `future date is rejected`() {
        val result = action.selectPickedDate(today.plusDays(1))

        assertTrue(result is DailyDateSelectionAction.PickedDateResult.FutureDate)
    }

    @Test
    fun `valid date returns date change refresh plan`() {
        val selectedDate = today.minusDays(3)

        val result = action.selectPickedDate(selectedDate)
            as DailyDateSelectionAction.PickedDateResult.Selected

        assertEquals(selectedDate, result.date)
        assertEquals(DailyDashboardRefreshPlans.SELECTED_DATE_CHANGED, result.refreshPlan)
    }
}
