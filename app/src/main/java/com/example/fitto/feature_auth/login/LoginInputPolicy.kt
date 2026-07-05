package com.example.fitto.feature_auth.login

enum class LoginIdentifierIssue {
    REQUIRED,
    INVALID_EMAIL,
    USERNAME_TOO_SHORT
}

enum class LoginPasswordIssue {
    REQUIRED,
    TOO_SHORT
}

data class LoginInputValidation(
    val identifierIssue: LoginIdentifierIssue? = null,
    val passwordIssue: LoginPasswordIssue? = null
) {
    val isValid: Boolean
        get() = identifierIssue == null && passwordIssue == null
}

object LoginInputPolicy {
    fun validate(
        emailOrUsername: String,
        password: String,
        isValidEmail: (String) -> Boolean
    ): LoginInputValidation {
        val value = emailOrUsername.trim()
        val identifierIssue = when {
            value.isBlank() -> LoginIdentifierIssue.REQUIRED
            value.contains("@") && !isValidEmail(value) -> LoginIdentifierIssue.INVALID_EMAIL
            !value.contains("@") && value.removePrefix("@").length < 3 -> {
                LoginIdentifierIssue.USERNAME_TOO_SHORT
            }

            else -> null
        }
        val passwordIssue = when {
            password.isBlank() -> LoginPasswordIssue.REQUIRED
            password.length < 6 -> LoginPasswordIssue.TOO_SHORT
            else -> null
        }
        return LoginInputValidation(identifierIssue, passwordIssue)
    }
}
