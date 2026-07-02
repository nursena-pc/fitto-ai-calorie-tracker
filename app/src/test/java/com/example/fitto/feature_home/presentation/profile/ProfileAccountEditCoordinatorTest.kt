package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileAccountEditCoordinatorTest {

    @Test
    fun `display name update reports success`() = runBlocking {
        val updates = mutableListOf<String>()
        var successCount = 0
        val coordinator = coordinator(
            action = action(updateDisplayName = updates::add),
            onDisplayNameUpdated = { successCount += 1 }
        )

        coordinator.updateDisplayName("Nursena")
        awaitCondition { successCount == 1 }

        assertEquals(listOf("Nursena"), updates)
    }

    @Test
    fun `failed edit reports error without success callback`() = runBlocking {
        val failure = IllegalStateException("failed")
        val errors = mutableListOf<Throwable>()
        var successCount = 0
        val coordinator = coordinator(
            action = action(updateUsername = { throw failure }),
            onUsernameUpdated = { successCount += 1 },
            onFailure = errors::add
        )

        coordinator.updateUsername("fitto")
        awaitCondition { errors.isNotEmpty() }

        assertEquals(listOf(failure), errors)
        assertEquals(0, successCount)
    }

    @Test
    fun `email request uses language and returns pending request`() = runBlocking {
        val calls = mutableListOf<String>()
        val requests = mutableListOf<ProfilePendingEmailChange>()
        val coordinator = coordinator(
            action = action(
                reauthenticate = { calls += "reauth:$it" },
                updateEmail = { email, language -> calls += "email:$email:$language" }
            ),
            onEmailChangeRequested = requests::add
        )

        coordinator.requestEmailChange(" new@example.com ", "secret")
        awaitCondition { requests.isNotEmpty() }

        assertEquals(
            listOf("reauth:secret", "email:new@example.com:en"),
            calls
        )
        assertEquals("new@example.com", requests.single().email)
    }

    @Test
    fun `refresh forwards calculated account state`() = runBlocking {
        val results = mutableListOf<ProfileAccountEmailRefreshResult>()
        val coordinator = coordinator(
            action = action(
                sync = { EmailChangeSyncResult.VERIFIED },
                authEmail = { "user@example.com" },
                hasUser = { true }
            ),
            onAccountEmailRefreshed = results::add
        )

        coordinator.refreshAccountEmail()
        awaitCondition { results.isNotEmpty() }

        assertEquals("user@example.com", results.single().authEmail)
        assertEquals(EmailChangeSyncResult.VERIFIED, results.single().syncResult)
    }

    @Test
    fun `clear cancels in flight edit without reporting failure`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val errors = mutableListOf<Throwable>()
        var success = false
        val coordinator = coordinator(
            action = action(
                updateDisplayName = {
                    started.complete(Unit)
                    release.await()
                }
            ),
            onDisplayNameUpdated = { success = true },
            onFailure = errors::add
        )

        coordinator.updateDisplayName("Nursena")
        started.await()
        coordinator.clear()
        release.complete(Unit)
        repeat(5) { yield() }

        assertFalse(success)
        assertEquals(emptyList<Throwable>(), errors)
    }

    private fun CoroutineScope.coordinator(
        action: ProfileAccountEditAction,
        onDisplayNameUpdated: () -> Unit = {},
        onUsernameUpdated: () -> Unit = {},
        onEmailChangeRequested: (ProfilePendingEmailChange) -> Unit = {},
        onPasswordUpdated: () -> Unit = {},
        onAccountEmailRefreshed: (ProfileAccountEmailRefreshResult) -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) = ProfileAccountEditCoordinator(
        scope = this,
        action = action,
        languageCode = { "en" },
        onDisplayNameUpdated = onDisplayNameUpdated,
        onUsernameUpdated = onUsernameUpdated,
        onEmailChangeRequested = onEmailChangeRequested,
        onPasswordUpdated = onPasswordUpdated,
        onAccountEmailRefreshed = onAccountEmailRefreshed,
        onFailure = onFailure
    )

    private fun action(
        updateDisplayName: suspend (String) -> Unit = {},
        updateUsername: suspend (String) -> Unit = {},
        reauthenticate: suspend (String) -> Unit = {},
        updateEmail: suspend (String, String) -> Unit = { _, _ -> },
        updatePassword: suspend (String, String) -> Unit = { _, _ -> },
        sync: suspend () -> EmailChangeSyncResult = { EmailChangeSyncResult.WAITING },
        authEmail: () -> String = { "" },
        hasUser: () -> Boolean = { false }
    ) = ProfileAccountEditAction(
        updateDisplayName = updateDisplayName,
        updateUsername = updateUsername,
        reauthenticateWithPassword = reauthenticate,
        updateEmailAfterReauth = updateEmail,
        updatePasswordAfterReauth = updatePassword,
        syncCurrentAuthEmail = sync,
        reloadCurrentUserBestEffort = {},
        currentAuthEmail = authEmail,
        hasCurrentUser = hasUser,
        isPasswordProvider = { true },
        isGoogleProvider = { false }
    )

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(2_000L) {
            while (!condition()) {
                yield()
                delay(1L)
            }
        }
    }
}
