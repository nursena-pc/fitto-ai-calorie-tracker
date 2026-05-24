package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderDefaults
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import org.json.JSONArray
import org.json.JSONObject

object ReminderPreferenceStore {

    private const val PREF_NAME = "fitto_reminder_preferences"
    private const val KEY_REMINDERS = "reminders"

    private fun preferences(context: Context) =
        UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun hasSavedReminders(context: Context): Boolean {
        return preferences(context).contains(KEY_REMINDERS)
    }

    fun getReminders(context: Context): MutableList<ProfileReminderItem> {
        val raw = preferences(context).getString(KEY_REMINDERS, null)

        if (raw.isNullOrBlank()) {
            val defaults = ProfileReminderDefaults.defaultItems(context).toMutableList()
            saveReminders(context, defaults)
            return defaults
        }

        return runCatching {
            val array = JSONArray(raw)
            val result = mutableListOf<ProfileReminderItem>()

            for (index in 0 until array.length()) {
                result.add(array.getJSONObject(index).toReminderItem())
            }

            result
        }.getOrElse {
            val defaults = ProfileReminderDefaults.defaultItems(context).toMutableList()
            saveReminders(context, defaults)
            defaults
        }
    }

    fun saveReminders(
        context: Context,
        reminders: List<ProfileReminderItem>
    ) {
        val array = JSONArray()

        reminders.forEach { item ->
            array.put(item.toJson())
        }

        preferences(context)
            .edit()
            .putString(KEY_REMINDERS, array.toString())
            .apply()
    }

    fun upsertReminder(
        context: Context,
        item: ProfileReminderItem
    ) {
        val reminders = getReminders(context)
        val index = reminders.indexOfFirst { it.id == item.id }

        if (index >= 0) {
            reminders[index] = item
        } else {
            reminders.add(item)
        }

        saveReminders(context, reminders)
    }

    fun deleteReminder(
        context: Context,
        id: String
    ) {
        val reminders = getReminders(context)
            .filterNot { it.id == id }

        saveReminders(context, reminders)
    }

    fun setEnabled(
        context: Context,
        id: String,
        enabled: Boolean
    ) {
        val reminders = getReminders(context).map { item ->
            if (item.id == id) {
                item.copy(isEnabled = enabled)
            } else {
                item
            }
        }

        saveReminders(context, reminders)
    }


    fun clear(context: Context) {
        preferences(context).edit()
            .remove(KEY_REMINDERS)
            .apply()
    }

    private fun ProfileReminderItem.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("tag", tag)
            .put("category", category.name)
            .put("iconKind", iconKind.name)
            .put("timeText", timeText)
            .put("repeatText", repeatText)
            .put("isEnabled", isEnabled)
            .put("minuteOfDay", minuteOfDay)
            .put("repeatMode", repeatMode.name)
            .put("selectedDays", selectedDays.joinToString(","))
            .put("notifyMode", notifyMode.name)
            .put("soundName", soundName)
            .put("customSoundUri", customSoundUri ?: "")
            .put("soundVolumePercent", soundVolumePercent)
            .put("vibrationEnabled", vibrationEnabled)
            .put("isWaterInterval", isWaterInterval)
            .put("waterStartMinute", waterStartMinute)
            .put("waterEndMinute", waterEndMinute)
            .put("waterIntervalMinute", waterIntervalMinute)
    }

    private fun JSONObject.toReminderItem(): ProfileReminderItem {
        return ProfileReminderItem(
            id = optString("id"),
            title = optString("title"),
            tag = optString("tag"),
            category = enumValueOrDefault(
                value = optString("category"),
                default = ReminderCategory.CUSTOM
            ),
            iconKind = enumValueOrDefault(
                value = optString("iconKind"),
                default = ReminderIconKind.CUSTOM
            ),
            timeText = optString("timeText"),
            repeatText = optString("repeatText"),
            isEnabled = optBoolean("isEnabled", true),
            minuteOfDay = optInt("minuteOfDay", 9 * 60),
            repeatMode = enumValueOrDefault(
                value = optString("repeatMode"),
                default = ReminderRepeatMode.DAILY
            ),
            selectedDays = optString("selectedDays")
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
                .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) },
            notifyMode = enumValueOrDefault(
                value = optString("notifyMode"),
                default = ReminderNotifyMode.SOUND_AND_VIBRATION
            ),
            soundName = optString("soundName"),
            customSoundUri = optString("customSoundUri")
                .takeIf { it.isNotBlank() },
            soundVolumePercent = optInt("soundVolumePercent", 80).coerceIn(0, 100),
            vibrationEnabled = optBoolean("vibrationEnabled", true),
            isWaterInterval = optBoolean("isWaterInterval", false),
            waterStartMinute = optInt("waterStartMinute", 9 * 60),
            waterEndMinute = optInt("waterEndMinute", 22 * 60),
            waterIntervalMinute = optInt("waterIntervalMinute", 120)
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String,
        default: T
    ): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}