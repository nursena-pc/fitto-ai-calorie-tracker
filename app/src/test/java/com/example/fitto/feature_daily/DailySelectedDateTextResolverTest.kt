package com.example.fitto.feature_daily

import com.example.fitto.core.text.AppLocales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DailySelectedDateTextResolverTest {

    @Test
    fun resolve_marksDateAsTodayWhenItMatchesToday() {
        val result = DailySelectedDateTextResolver.resolve(
            selectedDate = LocalDate.of(2026, 6, 27),
            today = LocalDate.of(2026, 6, 27),
            locale = AppLocales.TURKISH
        )

        assertTrue(result.isToday)
    }

    @Test
    fun resolve_marksPastDateAsNotToday() {
        val result = DailySelectedDateTextResolver.resolve(
            selectedDate = LocalDate.of(2026, 6, 26),
            today = LocalDate.of(2026, 6, 27),
            locale = AppLocales.TURKISH
        )

        assertFalse(result.isToday)
    }

    @Test
    fun resolve_formatsDateWithGivenLocale() {
        val result = DailySelectedDateTextResolver.resolve(
            selectedDate = LocalDate.of(2026, 6, 27),
            today = LocalDate.of(2026, 6, 27),
            locale = Locale.US
        )

        assertEquals("27 June 2026, Saturday", result.formattedDate)
    }
}
