package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class ProfilePhotoAlbumDateFormatterTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val formatter = ProfilePhotoAlbumDateFormatter(
        locale = Locale.US,
        localTimeZone = utc,
        utcTimeZone = utc
    )

    @Test
    fun `positive timestamp uses local date key and medium date text`() {
        val timestamp = Instant.parse("2026-01-02T10:30:00Z").toEpochMilli()

        assertEquals("2026-01-02", formatter.dateKeyFor(timestamp))
        assertEquals("Jan 2, 2026", formatter.dateTextFor(timestamp, "Undated"))
    }

    @Test
    fun `non positive timestamp is undated`() {
        assertEquals(ProfilePhotoAlbumFilter.UNDATED, formatter.dateKeyFor(0L))
        assertEquals("Undated", formatter.dateTextFor(-1L, "Undated"))
    }

    @Test
    fun `picker date key round trips through utc millis`() {
        val dateKey = "2026-06-17"

        val millis = formatter.pickerUtcMillisForDateKey(dateKey)

        assertEquals(dateKey, formatter.dateKeyForPickerSelection(requireNotNull(millis)))
    }

    @Test
    fun `special and invalid filter keys resolve safely`() {
        assertNull(formatter.pickerUtcMillisForDateKey(ProfilePhotoAlbumFilter.ALL))
        assertNull(formatter.pickerUtcMillisForDateKey(ProfilePhotoAlbumFilter.UNDATED))
        assertEquals(
            "All dates",
            formatter.dateTextForDateKey(ProfilePhotoAlbumFilter.ALL, "All dates", "Undated")
        )
        assertEquals(
            "Undated",
            formatter.dateTextForDateKey(ProfilePhotoAlbumFilter.UNDATED, "All dates", "Undated")
        )
        assertEquals(
            "not-a-date",
            formatter.dateTextForDateKey("not-a-date", "All dates", "Undated")
        )
    }
}
