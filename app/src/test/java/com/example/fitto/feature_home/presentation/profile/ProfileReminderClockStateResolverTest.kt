package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileReminderClockStateResolverTest {

    private val resolver = ProfileReminderClockStateResolver { minute ->
        "${minute / 60}:${minute % 60}"
    }

    @Test
    fun `regular reminders become ordered clock markers`() {
        val result = resolver.resolve(
            reminders = listOf(
                reminderItem(id = "late", minuteOfDay = 12 * 60),
                reminderItem(id = "early", minuteOfDay = 8 * 60)
            ),
            currentMinuteOfDay = 9 * 60
        )

        assertEquals(listOf("early", "late"), result.markers.map { it.id })
        assertTrue(result.markers.first().isPast)
        assertTrue(result.markers.last().isNext)
        assertEquals("late", result.nextEntry?.id)
    }

    @Test
    fun `water interval expands into markers through inclusive end time`() {
        val result = resolver.resolve(
            reminders = listOf(
                reminderItem(
                    id = "water",
                    category = ReminderCategory.WATER,
                    isWaterInterval = true,
                    waterStartMinute = 9 * 60,
                    waterEndMinute = 13 * 60,
                    waterIntervalMinute = 120
                )
            ),
            currentMinuteOfDay = 10 * 60
        )

        assertEquals(
            listOf("water_water_540", "water_water_660", "water_water_780"),
            result.markers.map { it.id }
        )
        assertTrue(result.markers.all { it.isWater })
        assertEquals(11 * 60, result.nextEntry?.minuteOfDay)
    }

    @Test
    fun `next reminder wraps to first entry after final reminder`() {
        val result = resolver.resolve(
            reminders = listOf(
                reminderItem(id = "morning", minuteOfDay = 8 * 60),
                reminderItem(id = "evening", minuteOfDay = 18 * 60)
            ),
            currentMinuteOfDay = 23 * 60
        )

        assertEquals("morning", result.nextEntry?.id)
        assertTrue(result.markers.first().isNext)
        assertTrue(result.markers.all { it.isPast })
    }

    @Test
    fun `water interval below minimum uses safe thirty minute step`() {
        val result = resolver.resolve(
            reminders = listOf(
                reminderItem(
                    id = "water",
                    category = ReminderCategory.WATER,
                    isWaterInterval = true,
                    waterStartMinute = 9 * 60,
                    waterEndMinute = 10 * 60,
                    waterIntervalMinute = 0
                )
            ),
            currentMinuteOfDay = 8 * 60
        )

        assertEquals(listOf(540, 570, 600), result.markers.map { it.minuteOfDay })
        assertFalse(result.markers.first().isPast)
    }

    @Test
    fun `very large water interval stops without integer overflow`() {
        val result = resolver.resolve(
            reminders = listOf(
                reminderItem(
                    id = "water",
                    category = ReminderCategory.WATER,
                    isWaterInterval = true,
                    waterStartMinute = 9 * 60,
                    waterEndMinute = 22 * 60,
                    waterIntervalMinute = Int.MAX_VALUE
                )
            ),
            currentMinuteOfDay = 8 * 60
        )

        assertEquals(listOf(9 * 60), result.markers.map { it.minuteOfDay })
    }

    @Test
    fun `invalid reversed water range produces no markers`() {
        val result = resolver.resolve(
            reminders = listOf(
                reminderItem(
                    category = ReminderCategory.WATER,
                    isWaterInterval = true,
                    waterStartMinute = 18 * 60,
                    waterEndMinute = 8 * 60
                )
            ),
            currentMinuteOfDay = 9 * 60
        )

        assertTrue(result.markers.isEmpty())
        assertNull(result.nextEntry)
    }
}
