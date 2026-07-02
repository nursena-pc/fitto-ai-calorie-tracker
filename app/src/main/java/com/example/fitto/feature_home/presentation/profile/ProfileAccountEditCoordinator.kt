package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProfileAccountEditCoordinator(
    private val scope: CoroutineScope,
    private val action: ProfileAccountEditAction,
    private val languageCode: () -> String,
    private val onDisplayNameUpdated: () -> Unit,
    private val onUsernameUpdated: () -> Unit,
    private val onEmailChangeRequested: (ProfilePendingEmailChange) -> Unit,
    private val onPasswordUpdated: () -> Unit,
    private val onAccountEmailRefreshed: (ProfileAccountEmailRefreshResult) -> Unit,
    private val onFailure: (Throwable) -> Unit
) {
    private var editJob: Job? = null
    private var refreshJob: Job? = null

    fun currentEmail(): String = action.currentEmail()

    fun emailAuthenticationRoute(): ProfileAccountAuthenticationRoute {
        return action.emailAuthenticationRoute()
    }

    fun passwordAuthenticationRoute(): ProfileAccountAuthenticationRoute {
        return action.passwordAuthenticationRoute()
    }

    fun updateDisplayName(displayName: String) {
        launchEdit(
            operation = { action.updateDisplayName(displayName) },
            onSuccess = { onDisplayNameUpdated() }
        )
    }

    fun updateUsername(username: String) {
        launchEdit(
            operation = { action.updateUsername(username) },
            onSuccess = { onUsernameUpdated() }
        )
    }

    fun requestEmailChange(
        newEmail: String,
        password: String
    ) {
        launchEdit(
            operation = {
                action.requestEmailChange(
                    newEmail = newEmail,
                    password = password,
                    languageCode = languageCode()
                )
            },
            onSuccess = onEmailChangeRequested
        )
    }

    fun updatePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        launchEdit(
            operation = {
                action.updatePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            },
            onSuccess = { onPasswordUpdated() }
        )
    }

    fun refreshAccountEmail() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try {
                onAccountEmailRefreshed(action.refreshAccountEmail())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onFailure(error)
            }
        }
    }

    fun clear() {
        editJob?.cancel()
        refreshJob?.cancel()
        editJob = null
        refreshJob = null
    }

    private fun <T> launchEdit(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit
    ) {
        editJob?.cancel()
        editJob = scope.launch {
            try {
                onSuccess(operation())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onFailure(error)
            }
        }
    }
}
