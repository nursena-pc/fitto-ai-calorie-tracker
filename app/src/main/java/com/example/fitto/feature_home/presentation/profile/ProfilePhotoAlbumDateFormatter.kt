package com.example.fitto.feature_home.presentation.profile

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object ProfilePhotoAlbumFilter {
    const val ALL = "all"
    const val UNDATED = "undated"
}

internal class ProfilePhotoAlbumDateFormatter(
    private val locale: Locale = Locale.getDefault(),
    private val localTimeZone: TimeZone = TimeZone.getDefault(),
    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")
) {

    fun dateKeyFor(createdAt: Long): String {
        if (createdAt <= 0L) return ProfilePhotoAlbumFilter.UNDATED

        return SimpleDateFormat(DATE_KEY_PATTERN, locale).apply {
            timeZone = localTimeZone
        }.format(Date(createdAt))
    }

    fun dateTextFor(createdAt: Long, undatedText: String): String {
        if (createdAt <= 0L) return undatedText

        return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply {
            timeZone = localTimeZone
        }.format(Date(createdAt))
    }

    fun dateKeyForPickerSelection(utcMillis: Long): String {
        return utcDateKeyFormatter().format(Date(utcMillis))
    }

    fun pickerUtcMillisForDateKey(dateKey: String): Long? {
        if (dateKey == ProfilePhotoAlbumFilter.ALL || dateKey == ProfilePhotoAlbumFilter.UNDATED) {
            return null
        }

        return runCatching {
            utcDateKeyFormatter().parse(dateKey)?.time
        }.getOrNull()
    }

    fun dateTextForDateKey(
        dateKey: String,
        allText: String,
        undatedText: String
    ): String {
        if (dateKey == ProfilePhotoAlbumFilter.ALL) return allText
        if (dateKey == ProfilePhotoAlbumFilter.UNDATED) return undatedText

        val selectedDate = pickerUtcMillisForDateKey(dateKey)?.let(::Date)
            ?: return dateKey

        return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply {
            timeZone = utcTimeZone
        }.format(selectedDate)
    }

    private fun utcDateKeyFormatter(): SimpleDateFormat {
        return SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).apply {
            timeZone = utcTimeZone
        }
    }

    private companion object {
        const val DATE_KEY_PATTERN = "yyyy-MM-dd"
    }
}
