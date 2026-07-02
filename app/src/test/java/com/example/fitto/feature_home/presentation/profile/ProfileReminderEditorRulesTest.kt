package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderIconKind
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileReminderEditorRulesTest {

    @Test
    fun `water end advances by interval when start passes current end`() {
        assertEquals(
            14 * 60,
            ProfileReminderEditorRules.resolveWaterEndAfterStart(
                startMinute = 12 * 60,
                currentEndMinute = 11 * 60,
                intervalMinute = 120
            )
        )
    }

    @Test
    fun `water end is capped at final minute of day`() {
        assertEquals(
            23 * 60 + 59,
            ProfileReminderEditorRules.resolveWaterEndSelection(
                startMinute = 23 * 60,
                selectedEndMinute = 22 * 60,
                intervalMinute = 180
            )
        )
    }

    @Test
    fun `repeat presets resolve modes and selected days`() {
        assertEquals(
            ProfileReminderRepeatSelection(
                ReminderRepeatMode.WEEKDAYS,
                setOf(1, 2, 3, 4, 5)
            ),
            ProfileReminderEditorRules.resolveRepeatSelection(1, emptySet())
        )
        assertEquals(
            ProfileReminderRepeatSelection(
                ReminderRepeatMode.WEEKENDS,
                setOf(6, 7)
            ),
            ProfileReminderEditorRules.resolveRepeatSelection(2, emptySet())
        )
        assertEquals(
            ProfileReminderRepeatSelection(
                ReminderRepeatMode.ONCE,
                emptySet()
            ),
            ProfileReminderEditorRules.resolveRepeatSelection(5, setOf(3))
        )
    }

    @Test
    fun `weekly repeat keeps current days and falls back to monday`() {
        assertEquals(
            setOf(2, 4),
            ProfileReminderEditorRules.resolveRepeatSelection(4, setOf(2, 4)).selectedDays
        )
        assertEquals(
            setOf(1),
            ProfileReminderEditorRules.resolveRepeatSelection(4, emptySet()).selectedDays
        )
    }

    @Test
    fun `audible sound restores sound and vibration from silent modes`() {
        assertEquals(
            ReminderNotifyMode.SOUND_AND_VIBRATION,
            ProfileReminderEditorRules.notifyModeForAudibleSound(ReminderNotifyMode.SILENT)
        )
        assertEquals(
            ReminderNotifyMode.SOUND_ONLY,
            ProfileReminderEditorRules.notifyModeForAudibleSound(ReminderNotifyMode.SOUND_ONLY)
        )
    }

    @Test
    fun `saved item normalizes title sound volume days and vibration`() {
        val saved = ProfileReminderEditorRules.buildSavedItem(
            editingItem = reminderItem(id = "edit"),
            title = "   ",
            fallbackTitle = "Fallback",
            tag = "Water",
            category = ReminderCategory.WATER,
            iconKind = ReminderIconKind.WATER,
            timeText = "Every 2 hours",
            repeatText = "09:00 - 22:00",
            minuteOfDay = -5,
            repeatMode = ReminderRepeatMode.DAILY,
            selectedDays = setOf(0, 1, 7, 8),
            notifyMode = ReminderNotifyMode.VIBRATION_ONLY,
            soundName = "",
            fallbackSoundName = "Default",
            customSoundUri = "",
            soundVolumePercent = 140,
            isWaterInterval = true,
            waterStartMinute = -1,
            waterEndMinute = 2_000,
            waterIntervalMinute = 0
        )

        assertEquals("Fallback", saved.title)
        assertEquals("Default", saved.soundName)
        assertEquals(100, saved.soundVolumePercent)
        assertEquals(setOf(1, 7), saved.selectedDays)
        assertTrue(saved.vibrationEnabled)
        assertEquals(0, saved.minuteOfDay)
        assertEquals(0, saved.waterStartMinute)
        assertEquals(23 * 60 + 59, saved.waterEndMinute)
        assertEquals(1, saved.waterIntervalMinute)
        assertNull(saved.customSoundUri)
    }

    @Test
    fun `silent saved item disables vibration`() {
        val saved = ProfileReminderEditorRules.buildSavedItem(
            editingItem = reminderItem(),
            title = "Quiet",
            fallbackTitle = "Fallback",
            tag = "Custom",
            category = ReminderCategory.CUSTOM,
            iconKind = ReminderIconKind.CUSTOM,
            timeText = "09:00",
            repeatText = "Once",
            minuteOfDay = 9 * 60,
            repeatMode = ReminderRepeatMode.ONCE,
            selectedDays = emptySet(),
            notifyMode = ReminderNotifyMode.SILENT,
            soundName = "Silent",
            fallbackSoundName = "Default",
            customSoundUri = null,
            soundVolumePercent = 80,
            isWaterInterval = false,
            waterStartMinute = 9 * 60,
            waterEndMinute = 22 * 60,
            waterIntervalMinute = 120
        )

        assertFalse(saved.vibrationEnabled)
    }
}
