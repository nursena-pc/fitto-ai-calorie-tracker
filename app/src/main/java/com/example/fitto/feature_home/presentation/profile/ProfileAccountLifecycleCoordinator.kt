package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProfileAccountLifecycleCoordinator(
    private val scope: CoroutineScope,
    private val action: ProfileAccountLifecycleAction,
    private val onSessionMissing: () -> Unit,
    private val onPasswordAuthenticationRequired: () -> Unit,
    private val onGoogleAuthenticationRequired: () -> Unit,
    private val onUnsupportedProvider: () -> Unit,
    private val onReauthenticated: () -> Unit,
    private val onPasswordRejected: (Throwable) -> Unit,
    private val onPasswordAuthenticationFailed: (Throwable?) -> Unit,
    private val onGoogleAuthenticationFailed: (Throwable) -> Unit,
    private val onAccountDeleted: () -> Unit,
    private val onDeleteRequiresRecentLogin: (Throwable) -> Unit,
    private val onDeleteFailed: (Throwable) -> Unit,
    private val onLoggedOut: () -> Unit
) {
    private var operationJob: Job? = null
    private var operationGeneration: Long = 0L

    fun beginDeleteAuthentication() {
        when (action.deletionAuthenticationRoute()) {
            ProfileAccountDeletionAuthenticationRoute.SESSION_MISSING -> onSessionMissing()
            ProfileAccountDeletionAuthenticationRoute.PASSWORD -> onPasswordAuthenticationRequired()
            ProfileAccountDeletionAuthenticationRoute.GOOGLE -> onGoogleAuthenticationRequired()
            ProfileAccountDeletionAuthenticationRoute.UNSUPPORTED -> onUnsupportedProvider()
        }
    }

    fun reauthenticatePassword(password: String) {
        launchReplacing { generation ->
            when (val result = action.reauthenticatePassword(password)) {
                ProfileAccountReauthenticationResult.Success -> {
                    deliver(generation, onReauthenticated)
                }
                ProfileAccountReauthenticationResult.SessionUnavailable -> {
                    deliver(generation) { onPasswordAuthenticationFailed(null) }
                }
                is ProfileAccountReauthenticationResult.WrongPassword -> {
                    deliver(generation) { onPasswordRejected(result.error) }
                }
                is ProfileAccountReauthenticationResult.Failure -> {
                    deliver(generation) { onPasswordAuthenticationFailed(result.error) }
                }
            }
        }
    }

    fun reauthenticateGoogle() {
        launchReplacing { generation ->
            when (val result = action.reauthenticateGoogle()) {
                ProfileAccountReauthenticationResult.Success -> {
                    deliver(generation, onReauthenticated)
                }
                ProfileAccountReauthenticationResult.SessionUnavailable -> Unit
                is ProfileAccountReauthenticationResult.WrongPassword -> Unit
                is ProfileAccountReauthenticationResult.Failure -> {
                    deliver(generation) { onGoogleAuthenticationFailed(result.error) }
                }
            }
        }
    }

    fun deleteAccount(
        deletedUserName: String,
        deletedMessageText: String
    ) {
        launchReplacing { generation ->
            when (
                val result = action.deleteAccount(
                    deletedUserName = deletedUserName,
                    deletedMessageText = deletedMessageText
                )
            ) {
                ProfileAccountDeletionResult.Success -> {
                    deliver(generation, onAccountDeleted)
                }
                is ProfileAccountDeletionResult.RequiresRecentLogin -> {
                    deliver(generation) { onDeleteRequiresRecentLogin(result.error) }
                }
                is ProfileAccountDeletionResult.Failure -> {
                    deliver(generation) { onDeleteFailed(result.error) }
                }
            }
        }
    }

    fun logout() {
        invalidateOperation()
        action.logout()
        onLoggedOut()
    }

    fun clear() {
        invalidateOperation()
    }

    private fun launchReplacing(block: suspend (generation: Long) -> Unit) {
        invalidateOperation()
        val generation = operationGeneration
        operationJob = scope.launch { block(generation) }
    }

    private fun deliver(generation: Long, callback: () -> Unit) {
        if (generation == operationGeneration) callback()
    }

    private fun invalidateOperation() {
        operationGeneration += 1L
        operationJob?.cancel()
        operationJob = null
    }
}
