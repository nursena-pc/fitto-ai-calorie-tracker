package com.example.fitto.feature_auth.login

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSignInCoordinatorTest {

    @Test
    fun signIn_usesAuthorizedCredentialFirst() = runBlocking {
        val credentialSource = FakeGoogleCredentialSource()
        val authSource = FakeGoogleAuthenticationSource()
        val coordinator = GoogleSignInCoordinator(credentialSource, authSource)

        coordinator.signIn()

        assertEquals(listOf(true), credentialSource.requests)
        assertEquals("authorized", authSource.signedInToken)
    }

    @Test
    fun signIn_retriesWithAllAccountsWhenAuthorizedCredentialMissing() = runBlocking {
        val credentialSource = FakeGoogleCredentialSource(failAuthorized = true)
        val authSource = FakeGoogleAuthenticationSource()
        val coordinator = GoogleSignInCoordinator(credentialSource, authSource)

        coordinator.signIn()

        assertEquals(listOf(true, false), credentialSource.requests)
        assertEquals("all-accounts", authSource.signedInToken)
    }

    @Test
    fun reauthenticate_usesSameFallbackStrategy() = runBlocking {
        val credentialSource = FakeGoogleCredentialSource(failAuthorized = true)
        val authSource = FakeGoogleAuthenticationSource()
        val coordinator = GoogleSignInCoordinator(credentialSource, authSource)

        coordinator.reauthenticateCurrentUser()

        assertEquals(listOf(true, false), credentialSource.requests)
        assertEquals("all-accounts", authSource.reauthenticatedToken)
    }

    private class FakeGoogleCredentialSource(
        private val failAuthorized: Boolean = false
    ) : GoogleCredentialSource {
        val requests = mutableListOf<Boolean>()

        override suspend fun requestIdToken(
            filterByAuthorizedAccounts: Boolean
        ): GoogleIdToken {
            requests += filterByAuthorizedAccounts
            if (filterByAuthorizedAccounts && failAuthorized) {
                throw GoogleCredentialUnavailableException()
            }
            return GoogleIdToken(
                if (filterByAuthorizedAccounts) "authorized" else "all-accounts"
            )
        }
    }

    private class FakeGoogleAuthenticationSource : GoogleAuthenticationSource {
        var signedInToken: String? = null
        var reauthenticatedToken: String? = null

        override suspend fun signIn(idToken: GoogleIdToken) {
            signedInToken = idToken.value
        }

        override suspend fun reauthenticateCurrentUser(idToken: GoogleIdToken) {
            reauthenticatedToken = idToken.value
        }
    }
}
