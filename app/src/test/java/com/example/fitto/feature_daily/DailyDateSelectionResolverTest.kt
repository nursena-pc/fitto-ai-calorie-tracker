package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyDateSelectionResolverTest {

    @Test
    fun resolveEntryDate_returnsNullForMissingEntryDate() {
        assertNull(
            DailyDateSelectionResolver.resolveEntryDate(
                currentDate = LocalDate.of(2026, 6, 27),
                entryDate = null
            )
        )
        assertNull(
            DailyDateSelectionResolver.resolveEntryDate(
                currentDate = LocalDate.of(2026, 6, 27),
                entryDate = "   "
            )
        )
    }

    @Test
    fun resolveEntryDate_usesParsedEntryDateWhenValid() {
        val selection = DailyDateSelectionResolver.resolveEntryDate(
            currentDate = LocalDate.of(2026, 6, 27),
            entryDate = "2026-06-20"
        )

        assertEquals(LocalDate.of(2026, 6, 20), selection?.date)
        assertEquals("2026-06-20", selection?.dateIso)
    }

    @Test
    fun resolveEntryDate_fallsBackToCurrentDateWhenInvalid() {
        val selection = DailyDateSelectionResolver.resolveEntryDate(
            currentDate = LocalDate.of(2026, 6, 27),
            entryDate = "not-a-date"
        )

        assertEquals(LocalDate.of(2026, 6, 27), selection?.date)
        assertEquals("2026-06-27", selection?.dateIso)
    }

    @Test
    fun resolvePickedDate_acceptsTodayAndPastDate() {
        val result = DailyDateSelectionResolver.resolvePickedDate(
            pickedDate = LocalDate.of(2026, 6, 26),
            today = LocalDate.of(2026, 6, 27)
        )

        val selected = result as DailyDateSelectionResolver.PickedDateResult.Selected
        assertEquals(LocalDate.of(2026, 6, 26), selected.selection.date)
    }

    @Test
    fun resolvePickedDate_blocksFutureDate() {
        val result = DailyDateSelectionResolver.resolvePickedDate(
            pickedDate = LocalDate.of(2026, 6, 28),
            today = LocalDate.of(2026, 6, 27)
        )

        assertTrue(result is DailyDateSelectionResolver.PickedDateResult.FutureDate)
    }
}
