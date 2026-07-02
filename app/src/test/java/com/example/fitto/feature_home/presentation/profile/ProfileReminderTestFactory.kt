package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode

internal fun reminderItem(
    id: String = "reminder",
    title: String = "Reminder",
    category: ReminderCategory = ReminderCategory.CUSTOM,
    isEnabled: Boolean = true,
    minuteOfDay: Int = 9 * 60,
    isWaterInterval: Boolean = false,
    waterStartMinute: Int = 9 * 60,
    waterEndMinute: Int = 22 * 60,
    waterIntervalMinute: Int = 120,
    notifyMode: ReminderNotifyMode = ReminderNotifyMode.SOUND_AND_VIBRATION,
    selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
): ProfileReminderItem {
    return ProfileReminderItem(
        id = id,
        title = title,
        tag = "Tag",
        category = category,
        iconKind = ReminderIconKind.CUSTOM,
        timeText = "09:00",
        repeatText = "Daily",
        isEnabled = isEnabled,
        minuteOfDay = minuteOfDay,
        repeatMode = ReminderRepeatMode.DAILY,
        selectedDays = selectedDays,
        notifyMode = notifyMode,
        soundName = "Default",
        isWaterInterval = isWaterInterval,
        waterStartMinute = waterStartMinute,
        waterEndMinute = waterEndMinute,
        waterIntervalMinute = waterIntervalMinute
    )
}
