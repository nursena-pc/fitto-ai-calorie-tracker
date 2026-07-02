package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode

object ProfileReminderEditorRules {

    fun resolveWaterEndAfterStart(
        startMinute: Int,
        currentEndMinute: Int,
        intervalMinute: Int
    ): Int {
        val normalizedStart = normalizeMinute(startMinute)
        val normalizedEnd = normalizeMinute(currentEndMinute)

        return if (normalizedEnd <= normalizedStart) {
            (normalizedStart + intervalMinute.coerceAtLeast(1)).coerceAtMost(LAST_MINUTE_OF_DAY)
        } else {
            normalizedEnd
        }
    }

    fun resolveWaterEndSelection(
        startMinute: Int,
        selectedEndMinute: Int,
        intervalMinute: Int
    ): Int {
        return resolveWaterEndAfterStart(
            startMinute = startMinute,
            currentEndMinute = selectedEndMinute,
            intervalMinute = intervalMinute
        )
    }

    fun resolveRepeatSelection(
        selectedIndex: Int,
        currentDays: Set<Int>
    ): ProfileReminderRepeatSelection {
        val validCurrentDays = currentDays.filterTo(linkedSetOf()) { it in 1..7 }

        return when (selectedIndex) {
            0 -> ProfileReminderRepeatSelection(
                mode = ReminderRepeatMode.DAILY,
                selectedDays = ALL_DAYS
            )

            1 -> ProfileReminderRepeatSelection(
                mode = ReminderRepeatMode.WEEKDAYS,
                selectedDays = WEEKDAYS
            )

            2 -> ProfileReminderRepeatSelection(
                mode = ReminderRepeatMode.WEEKENDS,
                selectedDays = WEEKENDS
            )

            4 -> ProfileReminderRepeatSelection(
                mode = ReminderRepeatMode.WEEKLY,
                selectedDays = validCurrentDays.ifEmpty { linkedSetOf(1) }
            )

            5 -> ProfileReminderRepeatSelection(
                mode = ReminderRepeatMode.ONCE,
                selectedDays = emptySet()
            )

            else -> ProfileReminderRepeatSelection(
                mode = ReminderRepeatMode.DAILY,
                selectedDays = validCurrentDays
            )
        }
    }

    fun notifyModeForAudibleSound(currentMode: ReminderNotifyMode): ReminderNotifyMode {
        return when (currentMode) {
            ReminderNotifyMode.SILENT,
            ReminderNotifyMode.VIBRATION_ONLY -> ReminderNotifyMode.SOUND_AND_VIBRATION

            else -> currentMode
        }
    }

    fun buildSavedItem(
        editingItem: ProfileReminderItem,
        title: String,
        fallbackTitle: String,
        tag: String,
        category: ReminderCategory,
        iconKind: ReminderIconKind,
        timeText: String,
        repeatText: String,
        minuteOfDay: Int,
        repeatMode: ReminderRepeatMode,
        selectedDays: Set<Int>,
        notifyMode: ReminderNotifyMode,
        soundName: String,
        fallbackSoundName: String,
        customSoundUri: String?,
        soundVolumePercent: Int,
        isWaterInterval: Boolean,
        waterStartMinute: Int,
        waterEndMinute: Int,
        waterIntervalMinute: Int
    ): ProfileReminderItem {
        return editingItem.copy(
            title = title.trim().ifBlank { fallbackTitle },
            tag = tag,
            category = category,
            iconKind = iconKind,
            timeText = timeText,
            repeatText = repeatText,
            minuteOfDay = normalizeMinute(minuteOfDay),
            repeatMode = repeatMode,
            selectedDays = selectedDays.filterTo(linkedSetOf()) { it in 1..7 },
            notifyMode = notifyMode,
            soundName = soundName.ifBlank { fallbackSoundName },
            customSoundUri = customSoundUri?.takeIf(String::isNotBlank),
            soundVolumePercent = soundVolumePercent.coerceIn(0, 100),
            vibrationEnabled = notifyMode == ReminderNotifyMode.SOUND_AND_VIBRATION ||
                notifyMode == ReminderNotifyMode.VIBRATION_ONLY,
            isWaterInterval = isWaterInterval,
            waterStartMinute = normalizeMinute(waterStartMinute),
            waterEndMinute = normalizeMinute(waterEndMinute),
            waterIntervalMinute = waterIntervalMinute.coerceAtLeast(1)
        )
    }

    private fun normalizeMinute(value: Int): Int = value.coerceIn(0, LAST_MINUTE_OF_DAY)

    private const val LAST_MINUTE_OF_DAY = 23 * 60 + 59
    private val ALL_DAYS = linkedSetOf(1, 2, 3, 4, 5, 6, 7)
    private val WEEKDAYS = linkedSetOf(1, 2, 3, 4, 5)
    private val WEEKENDS = linkedSetOf(6, 7)
}

data class ProfileReminderRepeatSelection(
    val mode: ReminderRepeatMode,
    val selectedDays: Set<Int>
)
