package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CancellationException

data class ProfileAccountSession(
    val email: String?,
    val hasPasswordProvider: Boolean,
    val hasGoogleProvider: Boolean
)

enum class ProfileAccountDeletionAuthenticationRoute {
    SESSION_MISSING,
    PASSWORD,
    GOOGLE,
    UNSUPPORTED
}

sealed interface ProfileAccountReauthenticationResult {
    data object Success : ProfileAccountReauthenticationResult
    data object SessionUnavailable : ProfileAccountReauthenticationResult
    data class WrongPassword(val error: Throwable) : ProfileAccountReauthenticationResult
    data class Failure(val error: Throwable) : ProfileAccountReauthenticationResult
}

sealed interface ProfileAccountDeletionResult {
    data object Success : ProfileAccountDeletionResult
    data class RequiresRecentLogin(val error: Throwable) : ProfileAccountDeletionResult
    data class Failure(val error: Throwable) : ProfileAccountDeletionResult
}

class ProfileAccountLifecycleAction(
    private val currentSession: () -> ProfileAccountSession?,
    private val reauthenticateWithPassword: suspend (email: String, password: String) -> Unit,
    private val reauthenticateWithGoogle: suspend () -> Unit,
    private val deleteCurrentAccount: suspend (
        deletedUserName: String,
        deletedMessageText: String
    ) -> Unit,
    private val clearAccountScopedPreferences: () -> Unit,
    private val clearReminderPreferences: () -> Unit,
    private val cancelActiveNotifications: () -> Unit,
    private val clearAccountNotifications: () -> Unit,
    private val signOut: () -> Unit,
    private val isInvalidCredentialsError: (Throwable) -> Boolean,
    private val isRecentLoginRequiredError: (Throwable) -> Boolean
) {

    fun deletionAuthenticationRoute(): ProfileAccountDeletionAuthenticationRoute {
        val session = currentSession()
            ?: return ProfileAccountDeletionAuthenticationRoute.SESSION_MISSING

        return when {
            session.hasPasswordProvider -> ProfileAccountDeletionAuthenticationRoute.PASSWORD
            session.hasGoogleProvider -> ProfileAccountDeletionAuthenticationRoute.GOOGLE
            else -> ProfileAccountDeletionAuthenticationRoute.UNSUPPORTED
        }
    }

    suspend fun reauthenticatePassword(
        password: String
    ): ProfileAccountReauthenticationResult {
        val email = currentSession()?.email?.trim().orEmpty()
        if (email.isBlank()) {
            return ProfileAccountReauthenticationResult.SessionUnavailable
        }

        return try {
            reauthenticateWithPassword(email, password)
            ProfileAccountReauthenticationResult.Success
        } catch (error: Throwable) {
            error.throwIfCancellation()
            if (isInvalidCredentialsError(error)) {
                ProfileAccountReauthenticationResult.WrongPassword(error)
            } else {
                ProfileAccountReauthenticationResult.Failure(error)
            }
        }
    }

    suspend fun reauthenticateGoogle(): ProfileAccountReauthenticationResult {
        return try {
            reauthenticateWithGoogle()
            ProfileAccountReauthenticationResult.Success
        } catch (error: Throwable) {
            error.throwIfCancellation()
            ProfileAccountReauthenticationResult.Failure(error)
        }
    }

    suspend fun deleteAccount(
        deletedUserName: String,
        deletedMessageText: String
    ): ProfileAccountDeletionResult {
        return try {
            deleteCurrentAccount(deletedUserName, deletedMessageText)
            clearAccountScopedPreferences()
            clearReminderPreferences()
            cancelActiveNotifications()
            clearAccountNotifications()
            ProfileAccountDeletionResult.Success
        } catch (error: Throwable) {
            error.throwIfCancellation()
            if (isRecentLoginRequiredError(error)) {
                ProfileAccountDeletionResult.RequiresRecentLogin(error)
            } else {
                ProfileAccountDeletionResult.Failure(error)
            }
        }
    }

    fun logout() {
        cancelActiveNotifications()
        clearAccountNotifications()
        signOut()
    }

    private fun Throwable.throwIfCancellation() {
        if (this is CancellationException) throw this
    }
}
