package com.example.fitto.feature_daily

import com.example.fitto.feature_sleep.data.SleepLogLocalModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySleepTimeSelectionTest {

    @Test
    fun from_returnsDefaultSleepWindowWhenLogIsMissing() {
        assertEquals(
            DailySleepTimeSelection(
                bedtimeHour = 23,
                bedtimeMinute = 0,
                wakeHour = 7,
                wakeMinute = 0
            ),
            DailySleepTimeSelection.from(null)
        )
    }

    @Test
    fun from_usesStoredSleepTimesWhenLogExists() {
        val log = SleepLogLocalModel(
            date = "2026-06-21",
            bedtimeHour = 22,
            bedtimeMinute = 30,
            wakeHour = 6,
            wakeMinute = 45,
            durationMinutes = 495,
            caloriesBurned = 500
        )

        assertEquals(
            DailySleepTimeSelection(
                bedtimeHour = 22,
                bedtimeMinute = 30,
                wakeHour = 6,
                wakeMinute = 45
            ),
            DailySleepTimeSelection.from(log)
        )
    }

    @Test
    fun withBedtimeAndWakeTime_updateOnlySelectedTimes() {
        val selection = DailySleepTimeSelection.from(null)
            .withBedtime(hour = 21, minute = 15)
            .withWakeTime(hour = 5, minute = 45)

        assertEquals(
            DailySleepTimeSelection(
                bedtimeHour = 21,
                bedtimeMinute = 15,
                wakeHour = 5,
                wakeMinute = 45
            ),
            selection
        )
    }

    @Test
    fun durationMinutes_handlesSleepWindowCrossingMidnight() {
        val selection = DailySleepTimeSelection(
            bedtimeHour = 22,
            bedtimeMinute = 30,
            wakeHour = 6,
            wakeMinute = 45
        )

        assertEquals(495, selection.durationMinutes())
    }

    @Test
    fun toLog_calculatesDurationAndCaloriesFromSelection() {
        val selection = DailySleepTimeSelection(
            bedtimeHour = 23,
            bedtimeMinute = 0,
            wakeHour = 7,
            wakeMinute = 0
        )

        assertEquals(
            SleepLogLocalModel(
                date = "2026-06-21",
                bedtimeHour = 23,
                bedtimeMinute = 0,
                wakeHour = 7,
                wakeMinute = 0,
                durationMinutes = 480,
                caloriesBurned = 532
            ),
            selection.toLog(date = "2026-06-21", weightKg = 70.0)
        )
    }
}
