package com.example.fitto.feature_auth.login

import java.util.Locale

enum class AuthLoginFailure {
    EMAIL_OR_USERNAME_REQUIRED,
    USERNAME_TOO_SHORT,
    USERNAME_NOT_FOUND,
    USERNAME_PROFILE_MISSING,
    INVALID_CREDENTIALS,
    ACCOUNT_NOT_FOUND,
    NETWORK,
    USERNAME_LOOKUP_UNKNOWN,
    UNKNOWN
}

enum class PasswordResetFailure {
    ACCOUNT_NOT_FOUND,
    INVALID_EMAIL,
    NETWORK,
    UNKNOWN
}

sealed interface AuthLoginResult {
    data object Success : AuthLoginResult
    data class Failure(val reason: AuthLoginFailure) : AuthLoginResult
}

sealed interface PasswordResetResult {
    data object Success : PasswordResetResult
    data class Failure(val reason: PasswordResetFailure) : PasswordResetResult
}

sealed interface UsernameEmailLookupResult {
    data class Found(val email: String) : UsernameEmailLookupResult
    data object NotFound : UsernameEmailLookupResult
    data object MissingEmail : UsernameEmailLookupResult
    data class Failure(val reason: AuthLoginFailure) : UsernameEmailLookupResult
}

data class AuthSessionSnapshot(
    val uid: String,
    val isPasswordUser: Boolean,
    val isEmailVerified: Boolean
)

object AuthUsernameNormalizer {
    fun normalizeForLoginLookup(value: String): String {
        return normalizeBase(value).trim()
    }

    fun normalizeForCreation(value: String): String {
        return normalizeBase(value).trim('.', '_')
    }

    private fun normalizeBase(value: String): String {
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
    }
}
