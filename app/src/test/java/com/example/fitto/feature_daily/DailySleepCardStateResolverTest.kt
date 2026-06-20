package com.example.fitto.feature_daily

import com.example.fitto.feature_sleep.data.SleepLogLocalModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySleepCardStateResolverTest {

    @Test
    fun resolve_returnsEmptyWhenLogIsMissing() {
        assertEquals(
            DailySleepCardStateResolver.State.Empty,
            DailySleepCardStateResolver.resolve(null)
        )
    }

    @Test
    fun resolve_mapsLogToLoggedState() {
        val log = SleepLogLocalModel(
            date = "2026-06-21",
            bedtimeHour = 22,
            bedtimeMinute = 10,
            wakeHour = 6,
            wakeMinute = 40,
            durationMinutes = 510,
            caloriesBurned = 520
        )

        assertEquals(
            DailySleepCardStateResolver.State.Logged(
                durationMinutes = 510,
                selection = DailySleepTimeSelection(
                    bedtimeHour = 22,
                    bedtimeMinute = 10,
                    wakeHour = 6,
                    wakeMinute = 40
                )
            ),
            DailySleepCardStateResolver.resolve(log)
        )
    }
}
