package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileEmailChangeCoordinatorTest {

    @Test
    fun `verified check stays active until presentation is stopped`() = runBlocking {
        val states = mutableListOf<ProfileEmailChangeState>()
        val coordinator = coordinator(
            check = { EmailChangeSyncResult.VERIFIED },
            states = states
        )

        coordinator.start(ProfilePendingEmailChange("user@example.com", "secret"))
        awaitCondition { states.lastOrNull() == ProfileEmailChangeState.Verified }

        assertEquals(
            listOf(ProfileEmailChangeState.Checking, ProfileEmailChangeState.Verified),
            states
        )
        assertTrue(coordinator.isRecoveryActive)
        coordinator.stop()
        assertFalse(coordinator.isRecoveryActive)
    }

    @Test
    fun `signed out password account restores session`() = runBlocking {
        val states = mutableListOf<ProfileEmailChangeState>()
        val restoredCredentials = mutableListOf<String>()
        val coordinator = coordinator(
            check = { EmailChangeSyncResult.SIGNED_OUT },
            restore = { email, password ->
                restoredCredentials += "$email:$password"
                EmailChangeSyncResult.VERIFIED
            },
            states = states
        )

        coordinator.start(ProfilePendingEmailChange("user@example.com", "secret"))
        awaitCondition { states.lastOrNull() == ProfileEmailChangeState.Verified }

        assertEquals(listOf("user@example.com:secret"), restoredCredentials)
        assertTrue(ProfileEmailChangeState.RestoringSession in states)
        assertTrue(coordinator.isRecoveryActive)
        coordinator.stop()
        assertFalse(coordinator.isRecoveryActive)
    }

    @Test
    fun `signed out request without password reports session lost`() = runBlocking {
        val states = mutableListOf<ProfileEmailChangeState>()
        val coordinator = coordinator(
            check = { EmailChangeSyncResult.SIGNED_OUT },
            states = states
        )

        coordinator.start(ProfilePendingEmailChange("user@example.com", null))
        awaitCondition { states.lastOrNull() == ProfileEmailChangeState.SessionLost }

        assertFalse(coordinator.isRecoveryActive)
    }

    @Test
    fun `recoverable check failure keeps polling`() = runBlocking {
        val states = mutableListOf<ProfileEmailChangeState>()
        val failures = mutableListOf<Throwable>()
        var checkCount = 0
        val coordinator = coordinator(
            check = {
                checkCount += 1
                if (checkCount == 1) error("temporary")
                EmailChangeSyncResult.VERIFIED
            },
            states = states,
            checkFailures = failures
        )

        coordinator.start(ProfilePendingEmailChange("user@example.com", "secret"))
        awaitCondition { states.lastOrNull() == ProfileEmailChangeState.Verified }

        assertEquals(1, failures.size)
        assertEquals("temporary", failures.single().message)
        assertEquals(2, checkCount)
    }

    @Test
    fun `resend disables button during cooldown and restores it`() = runBlocking {
        val states = mutableListOf<ProfileEmailChangeState>()
        val availability = mutableListOf<Boolean>()
        val resendRequests = mutableListOf<String>()
        val coordinator = coordinator(
            check = { EmailChangeSyncResult.WAITING },
            resend = { email, language -> resendRequests += "$email:$language" },
            states = states,
            availability = availability,
            pollIntervalMillis = 60_000L,
            resendCooldownMillis = 0L
        )

        coordinator.start(ProfilePendingEmailChange("user@example.com", "secret"))
        coordinator.resend("user@example.com")
        awaitCondition { availability == listOf(false, true) }

        assertEquals(listOf("user@example.com:tr"), resendRequests)
        assertTrue(ProfileEmailChangeState.Resent in states)
        coordinator.stop()
    }

    @Test
    fun `starting a new request suppresses stale completion`() = runBlocking {
        val firstResult = CompletableDeferred<EmailChangeSyncResult>()
        val states = mutableListOf<ProfileEmailChangeState>()
        val coordinator = coordinator(
            check = { email ->
                if (email == "first@example.com") {
                    firstResult.await()
                } else {
                    EmailChangeSyncResult.VERIFIED
                }
            },
            states = states
        )

        coordinator.start(ProfilePendingEmailChange("first@example.com", "secret"))
        yield()
        coordinator.start(ProfilePendingEmailChange("second@example.com", "secret"))
        awaitCondition { states.lastOrNull() == ProfileEmailChangeState.Verified }
        firstResult.complete(EmailChangeSyncResult.VERIFIED)
        repeat(5) { yield() }

        assertEquals(1, states.count { it == ProfileEmailChangeState.Verified })
    }

    private fun CoroutineScope.coordinator(
        check: suspend (String) -> EmailChangeSyncResult,
        restore: suspend (String, String) -> EmailChangeSyncResult = { _, _ -> EmailChangeSyncResult.SIGNED_OUT },
        resend: suspend (String, String) -> Unit = { _, _ -> },
        states: MutableList<ProfileEmailChangeState>,
        availability: MutableList<Boolean> = mutableListOf(),
        checkFailures: MutableList<Throwable> = mutableListOf(),
        resendFailures: MutableList<Throwable> = mutableListOf(),
        pollIntervalMillis: Long = 0L,
        resendCooldownMillis: Long = 0L
    ): ProfileEmailChangeCoordinator {
        return ProfileEmailChangeCoordinator(
            scope = this,
            action = ProfileEmailChangeAction(
                checkAndSyncPendingEmailChange = check,
                signInAfterVerifiedEmailChange = restore,
                resendPendingEmailChangeVerification = resend
            ),
            languageCode = { "tr" },
            pollIntervalMillis = pollIntervalMillis,
            resendCooldownMillis = resendCooldownMillis,
            onStateChanged = states::add,
            onResendAvailabilityChanged = availability::add,
            onCheckFailed = checkFailures::add,
            onResendFailed = resendFailures::add
        )
    }

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(2_000L) {
            while (!condition()) {
                yield()
                delay(1L)
            }
        }
    }
}
