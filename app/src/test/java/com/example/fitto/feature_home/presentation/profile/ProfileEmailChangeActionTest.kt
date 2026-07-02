package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileEmailChangeActionTest {

    @Test
    fun `check trims pending email`() = runBlocking {
        var checkedEmail = ""
        val action = action(
            check = { email ->
                checkedEmail = email
                EmailChangeSyncResult.WAITING
            }
        )

        action.check(" pending@example.com ")

        assertEquals("pending@example.com", checkedEmail)
    }

    @Test
    fun `restore session forwards trimmed email and password`() = runBlocking {
        var credentials = ""
        val action = action(
            restore = { email, password ->
                credentials = "$email:$password"
                EmailChangeSyncResult.VERIFIED
            }
        )

        val result = action.restoreSession(" new@example.com ", "secret")

        assertEquals("new@example.com:secret", credentials)
        assertEquals(EmailChangeSyncResult.VERIFIED, result)
    }

    @Test
    fun `resend forwards language and trimmed email`() = runBlocking {
        var request = ""
        val action = action(
            resend = { email, language -> request = "$email:$language" }
        )

        action.resend(" new@example.com ", "en")

        assertEquals("new@example.com:en", request)
    }

    private fun action(
        check: suspend (String) -> EmailChangeSyncResult = { EmailChangeSyncResult.WAITING },
        restore: suspend (String, String) -> EmailChangeSyncResult = { _, _ -> EmailChangeSyncResult.WAITING },
        resend: suspend (String, String) -> Unit = { _, _ -> }
    ) = ProfileEmailChangeAction(
        checkAndSyncPendingEmailChange = check,
        signInAfterVerifiedEmailChange = restore,
        resendPendingEmailChangeVerification = resend
    )
}
