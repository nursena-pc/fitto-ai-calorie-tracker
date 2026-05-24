package com.example.fitto.feature_home.presentation.profile.reminders

import android.content.Context
import com.example.fitto.R
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class ReminderCategory {
    ALL,
    MEAL,
    WATER,
    EXERCISE,
    WEIGHT,
    CUSTOM
}

enum class ReminderIconKind {
    BREAKFAST,
    LUNCH,
    SNACK,
    WATER,
    EXERCISE,
    WEIGHT,
    CUSTOM
}

enum class ReminderRepeatMode {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    WEEKLY,
    ONCE
}

enum class ReminderNotifyMode {
    SOUND_AND_VIBRATION,
    SOUND_ONLY,
    VIBRATION_ONLY,
    SILENT
}

data class ProfileReminderItem(
    val id: String,
    val title: String,
    val tag: String,
    val category: ReminderCategory,
    val iconKind: ReminderIconKind,
    val timeText: String,
    val repeatText: String,
    val isEnabled: Boolean,
    val minuteOfDay: Int,
    val repeatMode: ReminderRepeatMode = ReminderRepeatMode.DAILY,
    val selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val notifyMode: ReminderNotifyMode = ReminderNotifyMode.SOUND_AND_VIBRATION,
    val soundName: String = "",
    val customSoundUri: String? = null,
    val soundVolumePercent: Int = 80,
    val vibrationEnabled: Boolean = true,
    val isWaterInterval: Boolean = false,
    val waterStartMinute: Int = 9 * 60,
    val waterEndMinute: Int = 22 * 60,
    val waterIntervalMinute: Int = 120
)

object ProfileReminderDefaults {

    fun newCustomReminder(context: Context): ProfileReminderItem {
        val currentMinute = currentMinuteOfDay()

        return ProfileReminderItem(
            id = UUID.randomUUID().toString(),
            title = context.getString(R.string.reminder_new_default_title),
            tag = context.getString(R.string.reminder_tag_custom),
            category = ReminderCategory.CUSTOM,
            iconKind = ReminderIconKind.CUSTOM,
            timeText = minuteToText(currentMinute),
            repeatText = context.getString(R.string.reminder_repeat_daily),
            isEnabled = true,
            minuteOfDay = currentMinute,
            soundName = context.getString(R.string.reminder_sound_default),
            waterStartMinute = currentMinute,
            waterEndMinute = (currentMinute + 4 * 60).coerceAtMost(23 * 60 + 59)
        )
    }

    private fun currentMinuteOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun minuteToText(minuteOfDay: Int): String {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    fun defaultItems(context: Context): List<ProfileReminderItem> {
        return listOf(
            ProfileReminderItem(
                id = "breakfast",
                title = context.getString(R.string.reminder_breakfast),
                tag = context.getString(R.string.reminder_tag_meal),
                category = ReminderCategory.MEAL,
                iconKind = ReminderIconKind.BREAKFAST,
                timeText = "08:00",
                repeatText = context.getString(R.string.reminder_repeat_daily),
                isEnabled = true,
                minuteOfDay = 8 * 60,
                soundName = context.getString(R.string.reminder_sound_default)
            ),
            ProfileReminderItem(
                id = "lunch",
                title = context.getString(R.string.reminder_lunch),
                tag = context.getString(R.string.reminder_tag_meal),
                category = ReminderCategory.MEAL,
                iconKind = ReminderIconKind.LUNCH,
                timeText = "12:30",
                repeatText = context.getString(R.string.reminder_repeat_daily),
                isEnabled = true,
                minuteOfDay = 12 * 60 + 30,
                soundName = context.getString(R.string.reminder_sound_default)
            ),
            ProfileReminderItem(
                id = "snack",
                title = context.getString(R.string.reminder_snack),
                tag = context.getString(R.string.reminder_tag_meal),
                category = ReminderCategory.MEAL,
                iconKind = ReminderIconKind.SNACK,
                timeText = "15:00",
                repeatText = context.getString(R.string.reminder_repeat_daily),
                isEnabled = true,
                minuteOfDay = 15 * 60,
                soundName = context.getString(R.string.reminder_sound_default)
            ),
            ProfileReminderItem(
                id = "water",
                title = context.getString(R.string.reminder_water),
                tag = context.getString(R.string.reminder_tag_water),
                category = ReminderCategory.WATER,
                iconKind = ReminderIconKind.WATER,
                timeText = context.getString(R.string.reminder_every_two_hours),
                repeatText = context.getString(R.string.reminder_repeat_daily),
                isEnabled = true,
                minuteOfDay = 11 * 60,
                soundName = context.getString(R.string.reminder_sound_soft),
                isWaterInterval = true,
                waterIntervalMinute = 120
            ),
            ProfileReminderItem(
                id = "exercise",
                title = context.getString(R.string.reminder_exercise),
                tag = context.getString(R.string.reminder_tag_exercise),
                category = ReminderCategory.EXERCISE,
                iconKind = ReminderIconKind.EXERCISE,
                timeText = "18:30",
                repeatText = context.getString(R.string.reminder_repeat_weekdays),
                isEnabled = true,
                minuteOfDay = 18 * 60 + 30,
                repeatMode = ReminderRepeatMode.WEEKDAYS,
                soundName = context.getString(R.string.reminder_sound_default)
            ),
            ProfileReminderItem(
                id = "weight",
                title = context.getString(R.string.reminder_weight_tracking),
                tag = context.getString(R.string.reminder_tag_weight),
                category = ReminderCategory.WEIGHT,
                iconKind = ReminderIconKind.WEIGHT,
                timeText = "08:00",
                repeatText = context.getString(
                    R.string.reminder_repeat_weekly_with_days,
                    context.getString(R.string.day_monday_short)
                ),
                isEnabled = true,
                minuteOfDay = 8 * 60,
                repeatMode = ReminderRepeatMode.WEEKLY,
                selectedDays = setOf(1),
                soundName = context.getString(R.string.reminder_sound_default)
            )
        )
    }
}