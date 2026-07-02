package com.example.fitto.feature_daily

import com.example.fitto.feature_health.HealthConnectDailySummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class DailyHealthConnectSummaryActionTest {

    @Test
    fun `load forwards requested date and returns summary`() = runBlocking {
        val requestedDate = LocalDate.of(2026, 7, 2)
        val expectedSummary = HealthConnectDailySummary(
            steps = 4_250,
            activeCaloriesKcal = 310,
            hasPermission = true,
            isAvailable = true
        )
        var receivedDate: LocalDate? = null
        val action = DailyHealthConnectSummaryAction { date ->
            receivedDate = date
            expectedSummary
        }

        val result = action.load(requestedDate)

        assertEquals(requestedDate, receivedDate)
        assertEquals(requestedDate, result.requestedDate)
        assertSame(expectedSummary, result.summary)
    }
}
