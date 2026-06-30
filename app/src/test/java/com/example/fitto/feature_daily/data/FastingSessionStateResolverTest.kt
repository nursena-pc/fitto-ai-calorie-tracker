package com.example.fitto.feature_daily.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FastingSessionStateResolverTest {

    @Test
    fun resolveEndsAtMillis_addsFastingHoursToStartTime() {
        val startedAt = 1_000L

        assertEquals(
            1_000L + 16L * HOUR_MILLIS,
            FastingSessionStateResolver.resolveEndsAtMillis(
                startedAtMillis = startedAt,
                fastingHours = 16
            )
        )
    }

    @Test
    fun resolveSession_returnsNullWhenStoredValuesAreMissingOrInvalid() {
        assertNull(
            FastingSessionStateResolver.resolveSession(
                planId = null,
                startedAtMillis = 1L,
                endsAtMillis = 2L,
                fastingHours = 16
            )
        )
        assertNull(
            FastingSessionStateResolver.resolveSession(
                planId = "fasting_16_8",
                startedAtMillis = 0L,
                endsAtMillis = 2L,
                fastingHours = 16
            )
        )
        assertNull(
            FastingSessionStateResolver.resolveSession(
                planId = "fasting_16_8",
                startedAtMillis = 1L,
                endsAtMillis = 0L,
                fastingHours = 16
            )
        )
        assertNull(
            FastingSessionStateResolver.resolveSession(
                planId = "fasting_16_8",
                startedAtMillis = 1L,
                endsAtMillis = 2L,
                fastingHours = 0
            )
        )
    }

    @Test
    fun resolveSession_buildsSessionWhenStoredValuesAreValid() {
        val session = FastingSessionStateResolver.resolveSession(
            planId = "fasting_18_6",
            startedAtMillis = 100L,
            endsAtMillis = 200L,
            fastingHours = 18
        )

        requireNotNull(session)
        assertEquals("fasting_18_6", session.planId)
        assertEquals(100L, session.startedAtMillis)
        assertEquals(200L, session.endsAtMillis)
        assertEquals(18, session.fastingHours)
    }

    @Test
    fun resolveRemainingMillis_returnsZeroWhenSessionIsMissingOrForDifferentPlan() {
        val session = FastingSessionStore.FastingSession(
            planId = "fasting_16_8",
            startedAtMillis = 1_000L,
            endsAtMillis = 10_000L,
            fastingHours = 16
        )

        assertEquals(
            0L,
            FastingSessionStateResolver.resolveRemainingMillis(
                session = null,
                requestedPlanId = "fasting_16_8",
                nowMillis = 2_000L
            )
        )
        assertEquals(
            0L,
            FastingSessionStateResolver.resolveRemainingMillis(
                session = session,
                requestedPlanId = "fasting_18_6",
                nowMillis = 2_000L
            )
        )
    }

    @Test
    fun resolveRemainingMillis_clampsElapsedSessionAtZero() {
        val session = FastingSessionStore.FastingSession(
            planId = "fasting_16_8",
            startedAtMillis = 1_000L,
            endsAtMillis = 10_000L,
            fastingHours = 16
        )

        assertEquals(
            8_000L,
            FastingSessionStateResolver.resolveRemainingMillis(
                session = session,
                requestedPlanId = "fasting_16_8",
                nowMillis = 2_000L
            )
        )
        assertEquals(
            0L,
            FastingSessionStateResolver.resolveRemainingMillis(
                session = session,
                requestedPlanId = "fasting_16_8",
                nowMillis = 12_000L
            )
        )
    }

    private companion object {
        private const val HOUR_MILLIS = 60L * 60L * 1000L
    }
}
