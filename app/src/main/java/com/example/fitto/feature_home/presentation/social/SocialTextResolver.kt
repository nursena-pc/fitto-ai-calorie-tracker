package com.example.fitto.feature_home.presentation.social

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object SocialTextResolver {

    private val turkishLocale: Locale
        get() = Locale("tr", "TR")

    fun createInitials(value: String): String {
        val parts = value
            .replace("@", "")
            .split(" ", ".", "_", "-")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val initials = parts
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase(turkishLocale) }
            .joinToString("")

        return initials.ifBlank {
            value.firstOrNull()?.uppercase(turkishLocale) ?: "F"
        }
    }

    fun formatRelativeTime(
        timeMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (timeMillis <= 0L) return ""

        val diffMillis = (nowMillis - timeMillis).coerceAtLeast(0L)
        val diffMinutes = diffMillis / MILLIS_PER_MINUTE
        val diffHours = diffMinutes / MINUTES_PER_HOUR
        val diffDays = diffHours / HOURS_PER_DAY

        return when {
            diffMinutes < 1L -> "şimdi"
            diffMinutes < 60L -> "$diffMinutes dk"
            diffHours < 24L -> "$diffHours sa"
            diffDays == 1L -> "Dün"
            diffDays < 7L -> "$diffDays gün"
            else -> SimpleDateFormat("dd MMM", turkishLocale).format(Date(timeMillis))
        }
    }

    fun formatClockTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return "şimdi"
        return SimpleDateFormat("HH:mm", turkishLocale).format(Date(timeMillis))
    }

    fun validateVisibleName(value: String) {
        if (value.length < MIN_VISIBLE_NAME_LENGTH || value.length > MAX_VISIBLE_NAME_LENGTH) {
            throw IllegalArgumentException("VISIBLE_NAME_LENGTH")
        }

        if (!VISIBLE_NAME_REGEX.matches(value)) {
            throw IllegalArgumentException("VISIBLE_NAME_INVALID")
        }

        if (value.startsWith('.') || value.startsWith('_') ||
            value.endsWith('.') || value.endsWith('_')
        ) {
            throw IllegalArgumentException("VISIBLE_NAME_INVALID")
        }
    }

    fun normalizeUsername(value: String): String {
        return value
            .removePrefix("@")
            .trim()
            .lowercase(Locale.ROOT)
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("\\s+"), ".")
            .replace(Regex("[^a-z0-9._]"), "")
            .replace(Regex("[.]{2,}"), ".")
            .replace(Regex("[_]{2,}"), "_")
            .trim()
    }

    private const val MILLIS_PER_MINUTE = 1000L * 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val HOURS_PER_DAY = 24L
    private const val MIN_VISIBLE_NAME_LENGTH = 3
    private const val MAX_VISIBLE_NAME_LENGTH = 20
    private val VISIBLE_NAME_REGEX = Regex("^[a-z0-9._]+$")
}
