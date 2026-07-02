package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAccountLifecycleActionTest {

    @Test
    fun `missing session routes directly to welcome`() {
        assertEquals(
            ProfileAccountDeletionAuthenticationRoute.SESSION_MISSING,
            action(session = { null }).deletionAuthenticationRoute()
        )
    }

    @Test
    fun `password provider takes priority over google provider`() {
        val route = action(
            session = {
                ProfileAccountSession(
                    email = "user@example.com",
                    hasPasswordProvider = true,
                    hasGoogleProvider = true
                )
            }
        ).deletionAuthenticationRoute()

        assertEquals(ProfileAccountDeletionAuthenticationRoute.PASSWORD, route)
    }

    @Test
    fun `google and unsupported providers have explicit routes`() {
        assertEquals(
            ProfileAccountDeletionAuthenticationRoute.GOOGLE,
            action(
                session = {
                    ProfileAccountSession("user@example.com", false, true)
                }
            ).deletionAuthenticationRoute()
        )
        assertEquals(
            ProfileAccountDeletionAuthenticationRoute.UNSUPPORTED,
            action(
                session = {
                    ProfileAccountSession("user@example.com", false, false)
                }
            ).deletionAuthenticationRoute()
        )
    }

    @Test
    fun `password reauthentication trims email and forwards password`() = runBlocking {
        val calls = mutableListOf<String>()
        val result = action(
            session = {
                ProfileAccountSession(" user@example.com ", true, false)
            },
            passwordReauthentication = { email, password ->
                calls += "$email:$password"
            }
        ).reauthenticatePassword("secret")

        assertEquals(ProfileAccountReauthenticationResult.Success, result)
        assertEquals(listOf("user@example.com:secret"), calls)
    }

    @Test
    fun `password reauthentication reports unavailable session`() = runBlocking {
        val result = action(session = { null }).reauthenticatePassword("secret")

        assertEquals(ProfileAccountReauthenticationResult.SessionUnavailable, result)
    }

    @Test
    fun `invalid password is classified separately`() = runBlocking {
        val failure = IllegalArgumentException("wrong password")
        val result = action(
            passwordReauthentication = { _, _ -> throw failure },
            invalidCredentials = { it === failure }
        ).reauthenticatePassword("wrong")

        assertEquals(ProfileAccountReauthenticationResult.WrongPassword(failure), result)
    }

    @Test
    fun `google reauthentication reports failures`() = runBlocking {
        val failure = IllegalStateException("google failed")
        val result = action(
            googleReauthentication = { throw failure }
        ).reauthenticateGoogle()

        assertEquals(ProfileAccountReauthenticationResult.Failure(failure), result)
    }

    @Test
    fun `successful deletion performs local cleanup in order`() = runBlocking {
        val calls = mutableListOf<String>()
        val result = action(
            deleteAccount = { name, message -> calls += "delete:$name:$message" },
            clearAccountPreferences = { calls += "preferences" },
            clearReminderPreferences = { calls += "reminders" },
            cancelNotifications = { calls += "cancel-notifications" },
            clearNotifications = { calls += "clear-notifications" }
        ).deleteAccount("Deleted User", "Deleted message")

        assertEquals(ProfileAccountDeletionResult.Success, result)
        assertEquals(
            listOf(
                "delete:Deleted User:Deleted message",
                "preferences",
                "reminders",
                "cancel-notifications",
                "clear-notifications"
            ),
            calls
        )
    }

    @Test
    fun `recent login failure does not run local cleanup`() = runBlocking {
        val failure = IllegalStateException("recent login required")
        var cleanupCalled = false
        val result = action(
            deleteAccount = { _, _ -> throw failure },
            clearAccountPreferences = { cleanupCalled = true },
            recentLoginRequired = { it === failure }
        ).deleteAccount("Deleted User", "Deleted message")

        assertEquals(ProfileAccountDeletionResult.RequiresRecentLogin(failure), result)
        assertTrue(!cleanupCalled)
    }

    @Test
    fun `generic deletion failure does not run local cleanup`() = runBlocking {
        val failure = IllegalStateException("delete failed")
        val calls = mutableListOf<String>()
        val result = action(
            deleteAccount = { _, _ -> throw failure },
            clearAccountPreferences = { calls += "preferences" }
        ).deleteAccount("Deleted User", "Deleted message")

        assertEquals(ProfileAccountDeletionResult.Failure(failure), result)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `logout clears notifications before signing out`() {
        val calls = mutableListOf<String>()
        action(
            cancelNotifications = { calls += "cancel" },
            clearNotifications = { calls += "clear" },
            signOut = { calls += "sign-out" }
        ).logout()

        assertEquals(listOf("cancel", "clear", "sign-out"), calls)
    }

    private fun action(
        session: () -> ProfileAccountSession? = {
            ProfileAccountSession("user@example.com", true, false)
        },
        passwordReauthentication: suspend (String, String) -> Unit = { _, _ -> },
        googleReauthentication: suspend () -> Unit = {},
        deleteAccount: suspend (String, String) -> Unit = { _, _ -> },
        clearAccountPreferences: () -> Unit = {},
        clearReminderPreferences: () -> Unit = {},
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
        clearAccountScopedPreferences = clearAccountPreferences,
        clearReminderPreferences = clearReminderPreferences,
        cancelActiveNotifications = cancelNotifications,
        clearAccountNotifications = clearNotifications,
        signOut = signOut,
        isInvalidCredentialsError = invalidCredentials,
        isRecentLoginRequiredError = recentLoginRequired
    )
}
