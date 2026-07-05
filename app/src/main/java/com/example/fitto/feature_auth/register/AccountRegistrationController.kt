package com.example.fitto.feature_auth.register

import java.util.Locale

enum class RegistrationEmailIssue {
    REQUIRED,
    INVALID
}

enum class RegistrationPasswordIssue {
    REQUIRED,
    TOO_SHORT,
    SPECIAL_CHARACTER_REQUIRED
}

enum class RegistrationPasswordRepeatIssue {
    REQUIRED,
    DOES_NOT_MATCH
}

data class AccountRegistrationValidation(
    val normalizedEmail: String,
    val emailIssue: RegistrationEmailIssue? = null,
    val passwordIssue: RegistrationPasswordIssue? = null,
    val passwordRepeatIssue: RegistrationPasswordRepeatIssue? = null
) {
    val isValid: Boolean
        get() = emailIssue == null &&
            passwordIssue == null &&
            passwordRepeatIssue == null
}

sealed interface AccountRegistrationSubmission {
    data class Ready(
        val email: String,
        val password: String
    ) : AccountRegistrationSubmission

    data class Invalid(
        val validation: AccountRegistrationValidation
    ) : AccountRegistrationSubmission

    data object AlreadySubmitting : AccountRegistrationSubmission
}

class AccountRegistrationController(
    private val minimumPasswordLength: Int = 6
) {

    var isSubmitting: Boolean = false
        private set

    fun prepareSubmission(
        email: String,
        password: String,
        passwordRepeat: String,
        isValidEmail: (String) -> Boolean
    ): AccountRegistrationSubmission {
        if (isSubmitting) return AccountRegistrationSubmission.AlreadySubmitting

        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val validation = AccountRegistrationValidation(
            normalizedEmail = normalizedEmail,
            emailIssue = when {
                normalizedEmail.isBlank() -> RegistrationEmailIssue.REQUIRED
                !isValidEmail(normalizedEmail) -> RegistrationEmailIssue.INVALID
                else -> null
            },
            passwordIssue = when {
                password.isBlank() -> RegistrationPasswordIssue.REQUIRED
                password.length < minimumPasswordLength -> RegistrationPasswordIssue.TOO_SHORT
                password.none { char -> !char.isLetterOrDigit() } -> {
                    RegistrationPasswordIssue.SPECIAL_CHARACTER_REQUIRED
                }

                else -> null
            },
            passwordRepeatIssue = when {
                passwordRepeat.isBlank() -> RegistrationPasswordRepeatIssue.REQUIRED
                password != passwordRepeat -> RegistrationPasswordRepeatIssue.DOES_NOT_MATCH
                else -> null
            }
        )

        if (!validation.isValid) {
            return AccountRegistrationSubmission.Invalid(validation)
        }

        isSubmitting = true
        return AccountRegistrationSubmission.Ready(normalizedEmail, password)
    }

    fun completeSubmission() {
        isSubmitting = false
    }
}
