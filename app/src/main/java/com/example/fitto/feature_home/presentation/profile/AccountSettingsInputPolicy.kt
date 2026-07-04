package com.example.fitto.feature_home.presentation.profile

import java.util.Locale

internal object AccountSettingsInputPolicy {

    fun normalizeDisplayName(value: String): String {
        val normalized = value.trim().replace(Regex("\\s+"), " ")

        when {
            normalized.length < MIN_DISPLAY_NAME_LENGTH -> {
                throw AccountSettingsException(AccountSettingsError.DISPLAY_NAME_TOO_SHORT)
            }

            normalized.length > MAX_DISPLAY_NAME_LENGTH -> {
                throw AccountSettingsException(AccountSettingsError.DISPLAY_NAME_TOO_LONG)
            }
        }

        return normalized
    }

    fun normalizeUsername(value: String): String {
        val normalized = value
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

        when {
            normalized.length < MIN_USERNAME_LENGTH -> {
                throw AccountSettingsException(AccountSettingsError.USERNAME_TOO_SHORT)
            }

            normalized.length > MAX_USERNAME_LENGTH -> {
                throw AccountSettingsException(AccountSettingsError.USERNAME_TOO_LONG)
            }

            !USERNAME_REGEX.matches(normalized) -> {
                throw AccountSettingsException(AccountSettingsError.USERNAME_INVALID)
            }

            normalized.startsWith('.') ||
                normalized.startsWith('_') ||
                normalized.endsWith('.') ||
                normalized.endsWith('_') -> {
                throw AccountSettingsException(AccountSettingsError.USERNAME_INVALID)
            }
        }

        return normalized
    }

    fun normalizeStoredUsername(value: String): String {
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

    fun normalizeEmail(value: String): String {
        val normalized = value.trim()

        if (normalized.isBlank() || !EMAIL_REGEX.matches(normalized)) {
            throw AccountSettingsException(AccountSettingsError.EMAIL_INVALID)
        }

        return normalized
    }

    fun requireCurrentPassword(password: String): String {
        if (password.isBlank()) {
            throw AccountSettingsException(AccountSettingsError.CURRENT_PASSWORD_EMPTY)
        }

        return password
    }

    fun validateNewPassword(
        newPassword: String,
        confirmPassword: String
    ) {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            throw AccountSettingsException(AccountSettingsError.NEW_PASSWORD_TOO_SHORT)
        }

        if (newPassword != confirmPassword) {
            throw AccountSettingsException(AccountSettingsError.PASSWORD_CONFIRM_MISMATCH)
        }
    }

    private const val MIN_DISPLAY_NAME_LENGTH = 2
    private const val MAX_DISPLAY_NAME_LENGTH = 40
    private const val MIN_USERNAME_LENGTH = 3
    private const val MAX_USERNAME_LENGTH = 20
    private const val MIN_PASSWORD_LENGTH = 6

    private val USERNAME_REGEX = Regex("^[a-z0-9._]+$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
}
