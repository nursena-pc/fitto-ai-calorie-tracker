package com.example.fitto.feature_dietplans

import java.util.Locale

data class CustomFastingPlanSchedule(
    val fastingHours: Int,
    val eatingHours: Int,
    val fastingStartHour: Int,
    val eatingStartHour: Int,
    val fastingStartText: String,
    val eatingStartText: String,
    val fastingEndText: String
)

object CustomFastingPlanRules {
    const val DEFAULT_FASTING_HOURS = 16
    const val FASTING_START_HOUR = 20
    val FASTING_HOURS_RANGE = 12..20

    fun sanitizeFastingHours(fastingHours: Int): Int {
        return fastingHours.coerceIn(FASTING_HOURS_RANGE)
    }

    fun eatingHoursFor(fastingHours: Int): Int {
        return HOURS_PER_DAY - sanitizeFastingHours(fastingHours)
    }

    fun resolveSchedule(
        fastingHours: Int,
        locale: Locale = Locale.getDefault()
    ): CustomFastingPlanSchedule {
        val safeFastingHours = sanitizeFastingHours(fastingHours)
        val eatingHours = eatingHoursFor(safeFastingHours)
        val eatingStartHour = (FASTING_START_HOUR + safeFastingHours) % HOURS_PER_DAY

        return CustomFastingPlanSchedule(
            fastingHours = safeFastingHours,
            eatingHours = eatingHours,
            fastingStartHour = FASTING_START_HOUR,
            eatingStartHour = eatingStartHour,
            fastingStartText = formatHour(FASTING_START_HOUR, locale),
            eatingStartText = formatHour(eatingStartHour, locale),
            fastingEndText = formatHour(FASTING_START_HOUR, locale)
        )
    }

    fun formatHour(hour: Int, locale: Locale = Locale.getDefault()): String {
        val normalizedHour = ((hour % HOURS_PER_DAY) + HOURS_PER_DAY) % HOURS_PER_DAY
        return String.format(locale, "%02d:00", normalizedHour)
    }

    private const val HOURS_PER_DAY = 24
}
