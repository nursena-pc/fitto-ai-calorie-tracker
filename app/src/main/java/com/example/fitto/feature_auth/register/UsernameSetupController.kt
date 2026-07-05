package com.example.fitto.feature_auth.register

import com.example.fitto.feature_auth.login.AuthUsernameNormalizer
import java.util.Locale

enum class UsernameValidationIssue {
    TOO_SHORT,
    TOO_LONG,
    INVALID_CHARACTERS
}

data class UsernameSetupState(
    val input: String = "",
    val validationIssue: UsernameValidationIssue? = null,
    val isAvailable: Boolean = false,
    val lastCheckedUsername: String = "",
    val isSubmitting: Boolean = false
) {
    val canContinue: Boolean
        get() = !isSubmitting &&
            validationIssue == null &&
            isAvailable &&
            lastCheckedUsername == input
}

data class UsernameInputUpdate(
    val state: UsernameSetupState,
    val shouldCheckAvailability: Boolean,
    val showSuggestions: Boolean
)

sealed interface UsernameSubmissionDecision {
    data class Ready(val username: String) : UsernameSubmissionDecision
    data class Invalid(val issue: UsernameValidationIssue) : UsernameSubmissionDecision
    data object NeedsAvailabilityCheck : UsernameSubmissionDecision
    data object AlreadySubmitting : UsernameSubmissionDecision
}

class UsernameSetupController(
    private val minimumLength: Int = 3,
    private val maximumLength: Int = 20
) {

    var state: UsernameSetupState = UsernameSetupState()
        private set

    fun onInputChanged(value: String): UsernameInputUpdate {
        val normalized = normalize(value)
        val issue = validate(normalized)
        state = state.copy(
            input = normalized,
            validationIssue = issue,
            isAvailable = false,
            lastCheckedUsername = ""
        )
        return UsernameInputUpdate(
            state = state,
            shouldCheckAvailability = normalized.isNotBlank() && issue == null,
            showSuggestions = normalized.isBlank()
        )
    }

    fun applyAvailability(
        checkedUsername: String,
        available: Boolean
    ): UsernameSetupState {
        val normalized = normalize(checkedUsername)
        if (normalized != state.input) return state

        state = state.copy(
            isAvailable = available,
            lastCheckedUsername = normalized
        )
        return state
    }

    fun applyAvailabilityFailure(): UsernameSetupState {
        state = state.copy(
            isAvailable = false,
            lastCheckedUsername = ""
        )
        return state
    }

    fun prepareSubmission(): UsernameSubmissionDecision {
        if (state.isSubmitting) return UsernameSubmissionDecision.AlreadySubmitting
        val issue = validate(state.input)
        if (issue != null) return UsernameSubmissionDecision.Invalid(issue)
        if (!state.isAvailable || state.lastCheckedUsername != state.input) {
            return UsernameSubmissionDecision.NeedsAvailabilityCheck
        }

        state = state.copy(isSubmitting = true)
        return UsernameSubmissionDecision.Ready(state.input)
    }

    fun completeSubmission(): UsernameSetupState {
        state = state.copy(isSubmitting = false)
        return state
    }

    fun markTaken(): UsernameSetupState {
        state = state.copy(
            isSubmitting = false,
            isAvailable = false,
            lastCheckedUsername = state.input
        )
        return state
    }

    fun normalize(value: String): String {
        return AuthUsernameNormalizer.normalizeForCreation(value)
    }

    fun validate(username: String): UsernameValidationIssue? {
        return when {
            username.length < minimumLength -> UsernameValidationIssue.TOO_SHORT
            username.length > maximumLength -> UsernameValidationIssue.TOO_LONG
            !USERNAME_REGEX.matches(username) -> UsernameValidationIssue.INVALID_CHARACTERS
            else -> null
        }
    }

    fun createBaseUsername(
        email: String?,
        displayName: String?
    ): String {
        val source = email
            ?.substringBefore("@")
            ?.takeIf(String::isNotBlank)
            ?: displayName?.takeIf(String::isNotBlank)
            ?: DEFAULT_USERNAME

        return normalize(source)
            .ifBlank { DEFAULT_USERNAME }
            .take(BASE_USERNAME_MAX_LENGTH)
    }

    fun suggestions(
        baseUsername: String,
        randomSuffixes: List<Int>
    ): List<String> {
        val normalizedBase = normalize(baseUsername).ifBlank { DEFAULT_USERNAME }
        val primary = listOf(
            normalizedBase,
            "$normalizedBase.fit",
            "${normalizedBase}_fit"
        ).map { normalize(it).take(maximumLength) }
            .filter(String::isNotBlank)
            .distinct()
            .toMutableList()

        randomSuffixes.forEach { suffix ->
            if (primary.size < SUGGESTION_COUNT) {
                primary += normalize("$DEFAULT_USERNAME$suffix").take(maximumLength)
            }
        }
        return primary.distinct().take(SUGGESTION_COUNT)
    }

    fun resolveDisplayName(
        onboardingName: String,
        authDisplayName: String?,
        email: String?,
        appName: String
    ): String {
        val source = onboardingName.trim().ifBlank {
            authDisplayName?.takeIf(String::isNotBlank)
                ?: email?.substringBefore("@")
                ?: appName
        }
        return source.replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.forLanguageTag("tr-TR"))
            } else {
                char.toString()
            }
        }
    }

    private companion object {
        val USERNAME_REGEX = Regex("^[a-z0-9._]+$")
        const val DEFAULT_USERNAME = "fitto_user"
        const val BASE_USERNAME_MAX_LENGTH = 16
        const val SUGGESTION_COUNT = 3
    }
}
