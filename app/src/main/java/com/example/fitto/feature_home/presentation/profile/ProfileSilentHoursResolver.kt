package com.example.fitto.feature_home.presentation.profile

import java.util.Locale

data class ProfileSilentHoursChoiceState(
    val currentValue: String,
    val defaultValue: String,
    val offValue: String,
    val customRangeSelected: Boolean
)

data class ProfileSilentHoursRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

object ProfileSilentHoursResolver {

    const val CHOOSE_RANGE_VALUE = "__choose_range__"

    fun choiceState(
        currentValue: String,
        defaultValue: String,
        offValue: String
    ): ProfileSilentHoursChoiceState {
        return ProfileSilentHoursChoiceState(
            currentValue = currentValue,
            defaultValue = defaultValue,
            offValue = offValue,
            customRangeSelected = currentValue != defaultValue && currentValue != offValue
        )
    }

    fun rangeFor(
        value: String,
        defaultStartHour: Int = 22,
        defaultStartMinute: Int = 0,
        defaultEndHour: Int = 7,
        defaultEndMinute: Int = 0
    ): ProfileSilentHoursRange {
        val start = hourPart(value, startPart = true)
        val end = hourPart(value, startPart = false)

        return ProfileSilentHoursRange(
            startHour = start?.first ?: defaultStartHour,
            startMinute = start?.second ?: defaultStartMinute,
            endHour = end?.first ?: defaultEndHour,
            endMinute = end?.second ?: defaultEndMinute
        )
    }

    fun formatRange(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): String {
        return "${formatHour(startHour, startMinute)} - ${formatHour(endHour, endMinute)}"
    }

    fun formatHour(hour: Int, minute: Int): String {
        return "%02d:%02d".format(
            Locale.US,
            hour.coerceIn(0, 23),
            minute.coerceIn(0, 59)
        )
    }

    private fun hourPart(value: String, startPart: Boolean): Pair<Int, Int>? {
        if (!value.contains("-")) return null

        val part = if (startPart) {
            value.substringBefore("-").trim()
        } else {
            value.substringAfter("-").trim()
        }

        val match = HOUR_PATTERN.matchEntire(part) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null

        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private val HOUR_PATTERN = Regex("(\\d{2}):(\\d{2})")
}
