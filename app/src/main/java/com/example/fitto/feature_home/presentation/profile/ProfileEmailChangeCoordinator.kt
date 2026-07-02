package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProfileEmailChangeCoordinator(
    private val scope: CoroutineScope,
    private val action: ProfileEmailChangeAction,
    private val languageCode: () -> String,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
    private val resendCooldownMillis: Long = DEFAULT_RESEND_COOLDOWN_MILLIS,
    private val onStateChanged: (ProfileEmailChangeState) -> Unit,
    private val onResendAvailabilityChanged: (Boolean) -> Unit,
    private val onCheckFailed: (Throwable) -> Unit,
    private val onResendFailed: (Throwable) -> Unit
) {
    private var pollingJob: Job? = null
    private var resendJob: Job? = null
    private var generation: Long = 0L

    var isRecoveryActive: Boolean = false
        private set

    fun start(request: ProfilePendingEmailChange) {
        pollingJob?.cancel()
        resendJob?.cancel()
        generation += 1L
        val requestGeneration = generation
        isRecoveryActive = true
        emitState(requestGeneration, ProfileEmailChangeState.Checking)

        pollingJob = scope.launch {
            while (isActive) {
                val result = try {
                    action.check(request.email)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    if (requestGeneration == generation) onCheckFailed(error)
                    EmailChangeSyncResult.WAITING
                }

                when (result) {
                    EmailChangeSyncResult.VERIFIED -> {
                        finish(requestGeneration, ProfileEmailChangeState.Verified)
                        break
                    }

                    EmailChangeSyncResult.WAITING -> {
                        emitState(requestGeneration, ProfileEmailChangeState.Checking)
                        delay(pollIntervalMillis)
                    }

                    EmailChangeSyncResult.SIGNED_OUT -> {
                        if (request.password.isNullOrBlank()) {
                            finish(requestGeneration, ProfileEmailChangeState.SessionLost)
                            break
                        }

                        emitState(requestGeneration, ProfileEmailChangeState.RestoringSession)
                        val recoveryResult = try {
                            action.restoreSession(
                                email = request.email,
                                password = request.password
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            if (requestGeneration == generation) onCheckFailed(error)
                            EmailChangeSyncResult.SIGNED_OUT
                        }

                        when (recoveryResult) {
                            EmailChangeSyncResult.VERIFIED -> {
                                finish(requestGeneration, ProfileEmailChangeState.Verified)
                                break
                            }

                            EmailChangeSyncResult.WAITING -> {
                                emitState(requestGeneration, ProfileEmailChangeState.Checking)
                                delay(pollIntervalMillis)
                            }

                            EmailChangeSyncResult.SIGNED_OUT -> {
                                finish(requestGeneration, ProfileEmailChangeState.SessionLost)
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    fun resend(email: String) {
        resendJob?.cancel()
        val requestGeneration = generation
        resendJob = scope.launch {
            if (requestGeneration == generation) onResendAvailabilityChanged(false)
            try {
                action.resend(
                    email = email,
                    languageCode = languageCode()
                )
                emitState(requestGeneration, ProfileEmailChangeState.Resent)
                delay(resendCooldownMillis)
                if (requestGeneration == generation) onResendAvailabilityChanged(true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (requestGeneration == generation) {
                    onResendAvailabilityChanged(true)
                    onResendFailed(error)
                }
            }
        }
    }

    fun stop() {
        generation += 1L
        pollingJob?.cancel()
        resendJob?.cancel()
        pollingJob = null
        resendJob = null
        isRecoveryActive = false
    }

    private fun finish(
        requestGeneration: Long,
        state: ProfileEmailChangeState
    ) {
        if (requestGeneration != generation) return
        if (state == ProfileEmailChangeState.SessionLost) {
            isRecoveryActive = false
        }
        onStateChanged(state)
    }

    private fun emitState(
        requestGeneration: Long,
        state: ProfileEmailChangeState
    ) {
        if (requestGeneration == generation) {
            onStateChanged(state)
        }
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MILLIS = 2_500L
        const val DEFAULT_RESEND_COOLDOWN_MILLIS = 30_000L
    }
}

sealed interface ProfileEmailChangeState {
    data object Checking : ProfileEmailChangeState
    data object RestoringSession : ProfileEmailChangeState
    data object Verified : ProfileEmailChangeState
    data object SessionLost : ProfileEmailChangeState
    data object Resent : ProfileEmailChangeState
}
