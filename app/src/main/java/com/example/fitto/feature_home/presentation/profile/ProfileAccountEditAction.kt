package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CancellationException

class ProfileAccountEditAction(
    private val updateDisplayName: suspend (String) -> Unit,
    private val updateUsername: suspend (String) -> Unit,
    private val reauthenticateWithPassword: suspend (String) -> Unit,
    private val updateEmailAfterReauth: suspend (newEmail: String, languageCode: String) -> Unit,
    private val updatePasswordAfterReauth: suspend (newPassword: String, confirmPassword: String) -> Unit,
    private val syncCurrentAuthEmail: suspend () -> EmailChangeSyncResult,
    private val reloadCurrentUserBestEffort: suspend () -> Unit,
    private val currentAuthEmail: () -> String,
    private val hasCurrentUser: () -> Boolean,
    private val isPasswordProvider: () -> Boolean,
    private val isGoogleProvider: () -> Boolean
) {
    suspend fun updateDisplayName(value: String) {
        updateDisplayName.invoke(value)
    }

    suspend fun updateUsername(value: String) {
        updateUsername.invoke(value)
    }

    suspend fun requestEmailChange(
        newEmail: String,
        password: String,
        languageCode: String
    ): ProfilePendingEmailChange {
        val cleanEmail = newEmail.trim()
        reauthenticateWithPassword(password)
        updateEmailAfterReauth(cleanEmail, languageCode)
        return ProfilePendingEmailChange(
            email = cleanEmail,
            password = password
        )
    }

    suspend fun updatePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        reauthenticateWithPassword(currentPassword)
        updatePasswordAfterReauth(newPassword, confirmPassword)
    }

    suspend fun refreshAccountEmail(): ProfileAccountEmailRefreshResult {
        var syncFailure: Throwable? = null
        val syncResult = try {
            syncCurrentAuthEmail()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            syncFailure = error
            reloadCurrentUserBestEffort()
            EmailChangeSyncResult.WAITING
        }

        return ProfileAccountEmailRefreshResult(
            syncResult = syncResult,
            authEmail = currentAuthEmail().trim(),
            hasCurrentUser = hasCurrentUser(),
            syncFailure = syncFailure
        )
    }

    fun currentEmail(): String = currentAuthEmail()

    fun emailAuthenticationRoute(): ProfileAccountAuthenticationRoute {
        return when {
            isPasswordProvider() -> ProfileAccountAuthenticationRoute.PASSWORD
            isGoogleProvider() -> ProfileAccountAuthenticationRoute.GOOGLE_MANAGED
            else -> ProfileAccountAuthenticationRoute.UNSUPPORTED
        }
    }

    fun passwordAuthenticationRoute(): ProfileAccountAuthenticationRoute {
        return if (isPasswordProvider()) {
            ProfileAccountAuthenticationRoute.PASSWORD
        } else {
            // Preserve the existing account screen behavior: every non-password account
            // is presented as externally managed when password changes are requested.
            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED
        }
    }
}

data class ProfilePendingEmailChange(
    val email: String,
    val password: String?
)

data class ProfileAccountEmailRefreshResult(
    val syncResult: EmailChangeSyncResult,
    val authEmail: String,
    val hasCurrentUser: Boolean,
    val syncFailure: Throwable?
) {
    val shouldRefreshProfile: Boolean
        get() = syncResult != EmailChangeSyncResult.SIGNED_OUT && hasCurrentUser
}

enum class ProfileAccountAuthenticationRoute {
    PASSWORD,
    GOOGLE_MANAGED,
    UNSUPPORTED
}
