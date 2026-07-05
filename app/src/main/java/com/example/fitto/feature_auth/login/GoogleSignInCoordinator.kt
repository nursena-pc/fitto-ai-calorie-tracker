package com.example.fitto.feature_auth.login

data class GoogleIdToken(
    val value: String
)

class GoogleCredentialUnavailableException(
    cause: Throwable? = null
) : Exception(cause)

interface GoogleCredentialSource {
    suspend fun requestIdToken(filterByAuthorizedAccounts: Boolean): GoogleIdToken
}

interface GoogleAuthenticationSource {
    suspend fun signIn(idToken: GoogleIdToken)
    suspend fun reauthenticateCurrentUser(idToken: GoogleIdToken)
}

class GoogleSignInCoordinator(
    private val credentialSource: GoogleCredentialSource,
    private val authenticationSource: GoogleAuthenticationSource
) {

    suspend fun signIn() {
        val token = requestWithAuthorizedAccountFallback()
        authenticationSource.signIn(token)
    }

    suspend fun reauthenticateCurrentUser() {
        val token = requestWithAuthorizedAccountFallback()
        authenticationSource.reauthenticateCurrentUser(token)
    }

    private suspend fun requestWithAuthorizedAccountFallback(): GoogleIdToken {
        return try {
            credentialSource.requestIdToken(filterByAuthorizedAccounts = true)
        } catch (_: GoogleCredentialUnavailableException) {
            credentialSource.requestIdToken(filterByAuthorizedAccounts = false)
        }
    }
}
