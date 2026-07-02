package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAccountEditActionTest {

    @Test
    fun `request email change reauthenticates before requesting verification`() = runBlocking {
        val calls = mutableListOf<String>()
        val action = action(
            reauthenticate = { password -> calls += "reauth:$password" },
            updateEmail = { email, language -> calls += "email:$email:$language" }
        )

        val result = action.requestEmailChange(
            newEmail = " new@example.com ",
            password = "secret",
            languageCode = "tr"
        )

        assertEquals(
            listOf("reauth:secret", "email:new@example.com:tr"),
            calls
        )
        assertEquals("new@example.com", result.email)
        assertEquals("secret", result.password)
    }

    @Test
    fun `password update reauthenticates before changing password`() = runBlocking {
        val calls = mutableListOf<String>()
        val action = action(
            reauthenticate = { calls += "reauth:$it" },
            updatePassword = { password, confirmation ->
                calls += "password:$password:$confirmation"
            }
        )

        action.updatePassword("current", "new-password", "new-password")

        assertEquals(
            listOf("reauth:current", "password:new-password:new-password"),
            calls
        )
    }

    @Test
    fun `refresh returns auth state when sync succeeds`() = runBlocking {
        var reloadCount = 0
        val action = action(
            sync = { EmailChangeSyncResult.VERIFIED },
            reload = { reloadCount += 1 },
            authEmail = { " user@example.com " },
            hasUser = { true }
        )

        val result = action.refreshAccountEmail()

        assertEquals(EmailChangeSyncResult.VERIFIED, result.syncResult)
        assertEquals("user@example.com", result.authEmail)
        assertTrue(result.shouldRefreshProfile)
        assertNull(result.syncFailure)
        assertEquals(0, reloadCount)
    }

    @Test
    fun `refresh falls back to reload when sync fails`() = runBlocking {
        val failure = IllegalStateException("sync failed")
        var reloadCount = 0
        val action = action(
            sync = { throw failure },
            reload = { reloadCount += 1 },
            authEmail = { "user@example.com" },
            hasUser = { true }
        )

        val result = action.refreshAccountEmail()

        assertEquals(EmailChangeSyncResult.WAITING, result.syncResult)
        assertEquals(1, reloadCount)
        assertEquals(failure, result.syncFailure)
        assertTrue(result.shouldRefreshProfile)
    }

    @Test
    fun `signed out refresh does not request profile refresh`() = runBlocking {
        val result = action(
            sync = { EmailChangeSyncResult.SIGNED_OUT },
            hasUser = { false }
        ).refreshAccountEmail()

        assertFalse(result.shouldRefreshProfile)
    }

    @Test
    fun `email route prioritizes password provider`() {
        val route = action(
            passwordProvider = { true },
            googleProvider = { true }
        ).emailAuthenticationRoute()

        assertEquals(ProfileAccountAuthenticationRoute.PASSWORD, route)
    }

    @Test
    fun `email route distinguishes google and unsupported providers`() {
        assertEquals(
            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED,
            action(googleProvider = { true }).emailAuthenticationRoute()
        )
        assertEquals(
            ProfileAccountAuthenticationRoute.UNSUPPORTED,
            action().emailAuthenticationRoute()
        )
    }

    @Test
    fun `password route preserves externally managed fallback`() {
        assertEquals(
            ProfileAccountAuthenticationRoute.GOOGLE_MANAGED,
            action().passwordAuthenticationRoute()
        )
    }

    private fun action(
        updateDisplayName: suspend (String) -> Unit = {},
        updateUsername: suspend (String) -> Unit = {},
        reauthenticate: suspend (String) -> Unit = {},
        updateEmail: suspend (String, String) -> Unit = { _, _ -> },
        updatePassword: suspend (String, String) -> Unit = { _, _ -> },
        sync: suspend () -> EmailChangeSyncResult = { EmailChangeSyncResult.WAITING },
        reload: suspend () -> Unit = {},
        authEmail: () -> String = { "" },
        hasUser: () -> Boolean = { false },
        passwordProvider: () -> Boolean = { false },
        googleProvider: () -> Boolean = { false }
    ) = ProfileAccountEditAction(
        updateDisplayName = updateDisplayName,
        updateUsername = updateUsername,
        reauthenticateWithPassword = reauthenticate,
        updateEmailAfterReauth = updateEmail,
        updatePasswordAfterReauth = updatePassword,
        syncCurrentAuthEmail = sync,
        reloadCurrentUserBestEffort = reload,
        currentAuthEmail = authEmail,
        hasCurrentUser = hasUser,
        isPasswordProvider = passwordProvider,
        isGoogleProvider = googleProvider
    )
}
