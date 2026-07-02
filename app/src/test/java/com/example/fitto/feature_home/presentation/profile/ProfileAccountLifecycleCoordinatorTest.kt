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

class ProfileAccountLifecycleCoordinatorTest {

    @Test
    fun `delete authentication route requests password dialog`() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = coordinator(
            action = action(),
            events = events
        )

        coordinator.beginDeleteAuthentication()

        assertEquals(listOf("password-required"), events)
    }

    @Test
    fun `missing session requests welcome navigation`() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = coordinator(
            action = action(session = { null }),
            events = events
        )

        coordinator.beginDeleteAuthentication()

        assertEquals(listOf("session-missing"), events)
    }

    @Test
    fun `successful password reauthentication opens final confirmation`() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = coordinator(action = action(), events = events)

        coordinator.reauthenticatePassword("secret")
        awaitCondition { events.isNotEmpty() }

        assertEquals(listOf("reauthenticated"), events)
    }

    @Test
    fun `wrong password has dedicated callback`() = runBlocking {
        val failure = IllegalArgumentException("wrong")
        val events = mutableListOf<String>()
        val coordinator = coordinator(
            action = action(
                passwordReauthentication = { _, _ -> throw failure },
                invalidCredentials = { it === failure }
            ),
            events = events
        )

        coordinator.reauthenticatePassword("wrong")
        awaitCondition { events.isNotEmpty() }

        assertEquals(listOf("password-rejected:wrong"), events)
    }

    @Test
    fun `successful deletion reports completion`() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = coordinator(action = action(), events = events)

        coordinator.deleteAccount("Deleted User", "Deleted message")
        awaitCondition { events.isNotEmpty() }

        assertEquals(listOf("deleted"), events)
    }

    @Test
    fun `recent login deletion failure restores confirmation path`() = runBlocking {
        val failure = IllegalStateException("recent")
        val events = mutableListOf<String>()
        val coordinator = coordinator(
            action = action(
                deleteAccount = { _, _ -> throw failure },
                recentLoginRequired = { it === failure }
            ),
            events = events
        )

        coordinator.deleteAccount("Deleted User", "Deleted message")
        awaitCondition { events.isNotEmpty() }

        assertEquals(listOf("recent-login:recent"), events)
    }

    @Test
    fun `logout clears session and reports navigation`() = runBlocking {
        val calls = mutableListOf<String>()
        val events = mutableListOf<String>()
        val coordinator = coordinator(
            action = action(
                cancelNotifications = { calls += "cancel" },
                clearNotifications = { calls += "clear" },
                signOut = { calls += "sign-out" }
            ),
            events = events
        )

        coordinator.logout()

        assertEquals(listOf("cancel", "clear", "sign-out"), calls)
        assertEquals(listOf("logged-out"), events)
    }

    @Test
    fun `clear cancels in flight reauthentication without callbacks`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val coordinator = coordinator(
            action = action(
                googleReauthentication = {
                    started.complete(Unit)
                    release.await()
                }
            ),
            events = events
        )

        coordinator.reauthenticateGoogle()
        started.await()
        coordinator.clear()
        release.complete(Unit)
        repeat(5) { yield() }

        assertFalse(events.isNotEmpty())
    }

    private fun CoroutineScope.coordinator(
        action: ProfileAccountLifecycleAction,
        events: MutableList<String>
    ) = ProfileAccountLifecycleCoordinator(
        scope = this,
        action = action,
        onSessionMissing = { events += "session-missing" },
        onPasswordAuthenticationRequired = { events += "password-required" },
        onGoogleAuthenticationRequired = { events += "google-required" },
        onUnsupportedProvider = { events += "unsupported" },
        onReauthenticated = { events += "reauthenticated" },
        onPasswordRejected = { events += "password-rejected:${it.message}" },
        onPasswordAuthenticationFailed = { events += "password-failed:${it?.message}" },
        onGoogleAuthenticationFailed = { events += "google-failed:${it.message}" },
        onAccountDeleted = { events += "deleted" },
        onDeleteRequiresRecentLogin = { events += "recent-login:${it.message}" },
        onDeleteFailed = { events += "delete-failed:${it.message}" },
        onLoggedOut = { events += "logged-out" }
    )

    private fun action(
        session: () -> ProfileAccountSession? = {
            ProfileAccountSession("user@example.com", true, false)
        },
        passwordReauthentication: suspend (String, String) -> Unit = { _, _ -> },
        googleReauthentication: suspend () -> Unit = {},
        deleteAccount: suspend (String, String) -> Unit = { _, _ -> },
        cancelNotifications: () -> Unit = {},
        clearNotifications: () -> Unit = {},
        signOut: () -> Unit = {},
        invalidCredentials: (Throwable) -> Boolean = { false },
        recentLoginRequired: (Throwable) -> Boolean = { false }
    ) = ProfileAccountLifecycleAction(
        currentSession = session,
        reauthenticateWithPassword = passwordReauthentication,
        reauthenticateWithGoogle = googleReauthentication,
        deleteCurrentAccount = deleteAccount,
        clearAccountScopedPreferences = {},
        clearReminderPreferences = {},
        cancelActiveNotifications = cancelNotifications,
        clearAccountNotifications = clearNotifications,
        signOut = signOut,
        isInvalidCredentialsError = invalidCredentials,
        isRecentLoginRequiredError = recentLoginRequired
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
