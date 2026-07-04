package com.example.fitto.feature_home.presentation.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSettingsRepositoryTest {

    @Test
    fun `identity operations are routed to identity source`() = runBlocking {
        val identity = FakeIdentitySettingsSource()
        val repository = repository(identity = identity)

        repository.updateDisplayName("Ada Lovelace")
        repository.updateUsername("ada")

        assertEquals("Ada Lovelace", identity.displayName)
        assertEquals("ada", identity.username)
    }

    @Test
    fun `email operations preserve default language and results`() = runBlocking {
        val email = FakeEmailChangeSource().apply {
            currentEmail = "current@example.com"
            signedIn = true
            pendingResult = EmailChangeSyncResult.WAITING
            signInResult = EmailChangeSyncResult.VERIFIED
            syncResult = EmailChangeSyncResult.SIGNED_OUT
        }
        val repository = repository(email = email)

        repository.updateEmailAfterReauth("new@example.com")
        repository.resendPendingEmailChangeVerification("pending@example.com", "en")
        repository.reloadCurrentUserBestEffort()

        assertEquals("new@example.com", email.updatedEmail)
        assertEquals("tr", email.updatedEmailLanguage)
        assertEquals("pending@example.com", email.resentEmail)
        assertEquals("en", email.resentEmailLanguage)
        assertTrue(email.reloadCalled)
        assertEquals("current@example.com", repository.currentAuthEmail())
        assertTrue(repository.hasCurrentUser())
        assertEquals(
            EmailChangeSyncResult.WAITING,
            repository.checkAndSyncPendingEmailChange("new@example.com")
        )
        assertEquals("new@example.com", email.checkedEmail)
        assertEquals(
            EmailChangeSyncResult.VERIFIED,
            repository.signInAfterVerifiedEmailChange("new@example.com", "password")
        )
        assertEquals("password", email.signInPassword)
        assertEquals(
            EmailChangeSyncResult.SIGNED_OUT,
            repository.syncCurrentAuthEmailToProfileIfNeeded()
        )
        assertFalse(email.lastSkipReload)
    }

    @Test
    fun `email sync forwards explicit skip reload flag`() = runBlocking {
        val email = FakeEmailChangeSource()
        val repository = repository(email = email)

        repository.syncCurrentAuthEmailToProfileIfNeeded(skipReload = true)

        assertTrue(email.lastSkipReload)
    }

    @Test
    fun `security operations and provider queries are routed to security source`() = runBlocking {
        val security = FakeSecuritySettingsSource().apply {
            passwordProvider = true
            googleProvider = false
            historyUserId = "user-1"
        }
        val repository = repository(security = security)

        repository.reauthenticateWithPassword("current-password")
        repository.updatePasswordAfterReauth("new-password", "new-password")

        assertEquals("current-password", security.reauthPassword)
        assertEquals("new-password", security.newPassword)
        assertEquals("new-password", security.confirmPassword)
        assertTrue(repository.isPasswordProvider())
        assertFalse(repository.isGoogleProvider())
        assertEquals("user-1", repository.clearCurrentUserHistory())
    }

    private fun repository(
        identity: AccountIdentitySettingsSource = FakeIdentitySettingsSource(),
        email: AccountEmailChangeSource = FakeEmailChangeSource(),
        security: AccountSecuritySettingsSource = FakeSecuritySettingsSource()
    ): AccountSettingsRepository {
        return AccountSettingsRepository(
            identitySettingsSource = identity,
            emailChangeSource = email,
            securitySettingsSource = security
        )
    }

    private class FakeIdentitySettingsSource : AccountIdentitySettingsSource {
        var displayName: String? = null
        var username: String? = null

        override suspend fun updateDisplayName(displayName: String) {
            this.displayName = displayName
        }

        override suspend fun updateUsername(rawUsername: String) {
            username = rawUsername
        }
    }

    private class FakeEmailChangeSource : AccountEmailChangeSource {
        var updatedEmail: String? = null
        var updatedEmailLanguage: String? = null
        var resentEmail: String? = null
        var resentEmailLanguage: String? = null
        var currentEmail: String = ""
        var signedIn: Boolean = false
        var reloadCalled: Boolean = false
        var checkedEmail: String? = null
        var signInEmail: String? = null
        var signInPassword: String? = null
        var lastSkipReload: Boolean = false
        var pendingResult: EmailChangeSyncResult = EmailChangeSyncResult.WAITING
        var signInResult: EmailChangeSyncResult = EmailChangeSyncResult.WAITING
        var syncResult: EmailChangeSyncResult = EmailChangeSyncResult.WAITING

        override suspend fun updateEmailAfterReauth(newEmail: String, languageCode: String) {
            updatedEmail = newEmail
            updatedEmailLanguage = languageCode
        }

        override suspend fun resendPendingEmailChangeVerification(
            pendingEmail: String,
            languageCode: String
        ) {
            resentEmail = pendingEmail
            resentEmailLanguage = languageCode
        }

        override fun currentAuthEmail(): String = currentEmail

        override fun hasCurrentUser(): Boolean = signedIn

        override suspend fun reloadCurrentUserBestEffort() {
            reloadCalled = true
        }

        override suspend fun checkAndSyncPendingEmailChange(
            expectedEmail: String
        ): EmailChangeSyncResult {
            checkedEmail = expectedEmail
            return pendingResult
        }

        override suspend fun signInAfterVerifiedEmailChange(
            email: String,
            password: String
        ): EmailChangeSyncResult {
            signInEmail = email
            signInPassword = password
            return signInResult
        }

        override suspend fun syncCurrentAuthEmailToProfileIfNeeded(
            skipReload: Boolean
        ): EmailChangeSyncResult {
            lastSkipReload = skipReload
            return syncResult
        }
    }

    private class FakeSecuritySettingsSource : AccountSecuritySettingsSource {
        var reauthPassword: String? = null
        var newPassword: String? = null
        var confirmPassword: String? = null
        var passwordProvider: Boolean = false
        var googleProvider: Boolean = false
        var historyUserId: String = ""

        override suspend fun reauthenticateWithPassword(password: String) {
            reauthPassword = password
        }

        override suspend fun updatePasswordAfterReauth(
            newPassword: String,
            confirmPassword: String
        ) {
            this.newPassword = newPassword
            this.confirmPassword = confirmPassword
        }

        override fun isPasswordProvider(): Boolean = passwordProvider

        override fun isGoogleProvider(): Boolean = googleProvider

        override suspend fun clearCurrentUserHistory(): String = historyUserId
    }
}
