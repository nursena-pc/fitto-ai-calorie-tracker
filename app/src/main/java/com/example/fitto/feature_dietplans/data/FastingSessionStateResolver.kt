package com.example.fitto.feature_daily.data

object FastingSessionStateResolver {
    private const val HOUR_MILLIS = 60L * 60L * 1000L

    fun resolveEndsAtMillis(
        startedAtMillis: Long,
        fastingHours: Int
    ): Long {
        return startedAtMillis + fastingHours * HOUR_MILLIS
    }

    fun resolveSession(
        planId: String?,
        startedAtMillis: Long,
        endsAtMillis: Long,
        fastingHours: Int
    ): FastingSessionStore.FastingSession? {
        if (planId == null) return null
        if (startedAtMillis <= 0L || endsAtMillis <= 0L || fastingHours <= 0) return null

        return FastingSessionStore.FastingSession(
            planId = planId,
            startedAtMillis = startedAtMillis,
            endsAtMillis = endsAtMillis,
            fastingHours = fastingHours
        )
    }

    fun resolveRemainingMillis(
        session: FastingSessionStore.FastingSession?,
        requestedPlanId: String,
        nowMillis: Long
    ): Long {
        if (session == null || session.planId != requestedPlanId) return 0L

        return (session.endsAtMillis - nowMillis).coerceAtLeast(0L)
    }
}
