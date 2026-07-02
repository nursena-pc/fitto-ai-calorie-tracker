package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import java.util.Locale

class ProfileReminderClockStateResolver(
    private val minuteText: (Int) -> String = { minuteOfDay ->
        val normalized = minuteOfDay.coerceIn(0, MINUTES_IN_DAY - 1)
        String.format(
            Locale.getDefault(),
            "%02d:%02d",
            normalized / 60,
            normalized % 60
        )
    }
) {

    fun resolve(
        reminders: List<ProfileReminderItem>,
        currentMinuteOfDay: Int
    ): ProfileReminderClockState {
        val currentMinute = currentMinuteOfDay.coerceIn(0, MINUTES_IN_DAY - 1)
        val entries = reminders
            .flatMap(::entriesFor)
            .sortedBy(ProfileReminderClockEntryState::minuteOfDay)
        val nextEntry = entries.firstOrNull { it.minuteOfDay >= currentMinute }
            ?: entries.firstOrNull()

        return ProfileReminderClockState(
            markers = entries.map { entry ->
                ProfileReminderClockMarkerState(
                    id = entry.id,
                    minuteOfDay = entry.minuteOfDay,
                    category = entry.category,
                    title = entry.title,
                    timeText = entry.timeText,
                    isWater = entry.isWater,
                    isPast = entry.minuteOfDay < currentMinute,
                    isNext = entry.id == nextEntry?.id
                )
            },
            nextEntry = nextEntry
        )
    }

    private fun entriesFor(item: ProfileReminderItem): List<ProfileReminderClockEntryState> {
        if (!item.isWaterInterval) {
            return listOf(
                ProfileReminderClockEntryState(
                    id = item.id,
                    title = item.title,
                    minuteOfDay = item.minuteOfDay.coerceIn(0, MINUTES_IN_DAY - 1),
                    timeText = minuteText(item.minuteOfDay),
                    category = item.category,
                    isWater = false
                )
            )
        }

        val startMinute = item.waterStartMinute.coerceIn(0, MINUTES_IN_DAY - 1)
        val endMinute = item.waterEndMinute.coerceIn(0, MINUTES_IN_DAY - 1)
        val intervalMinute = item.waterIntervalMinute.coerceAtLeast(MIN_WATER_INTERVAL_MINUTES)

        if (startMinute > endMinute) return emptyList()

        return buildList {
            var current = startMinute

            while (current <= endMinute) {
                add(
                    ProfileReminderClockEntryState(
                        id = "${item.id}_water_$current",
                        title = item.title,
                        minuteOfDay = current,
                        timeText = minuteText(current),
                        category = ReminderCategory.WATER,
                        isWater = true
                    )
                )

                if (current > endMinute - intervalMinute) break
                current += intervalMinute
            }
        }
    }

    private companion object {
        const val MINUTES_IN_DAY = 24 * 60
        const val MIN_WATER_INTERVAL_MINUTES = 30
    }
}

data class ProfileReminderClockState(
    val markers: List<ProfileReminderClockMarkerState>,
    val nextEntry: ProfileReminderClockEntryState?
)

data class ProfileReminderClockMarkerState(
    val id: String,
    val minuteOfDay: Int,
    val category: ReminderCategory,
    val title: String,
    val timeText: String,
    val isWater: Boolean,
    val isPast: Boolean,
    val isNext: Boolean
)

data class ProfileReminderClockEntryState(
    val id: String,
    val title: String,
    val minuteOfDay: Int,
    val timeText: String,
    val category: ReminderCategory,
    val isWater: Boolean
)
