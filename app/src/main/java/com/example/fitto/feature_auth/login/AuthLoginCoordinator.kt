package com.example.fitto.feature_auth.login

class AuthLoginCoordinator(
    private val dataSource: AuthLoginDataSource
) {

    suspend fun signIn(
        emailOrUsername: String,
        password: String
    ): AuthLoginResult {
        val value = emailOrUsername.trim()
        if (value.isBlank()) {
            return AuthLoginResult.Failure(AuthLoginFailure.EMAIL_OR_USERNAME_REQUIRED)
        }

        if (value.contains("@")) {
            return dataSource.signInWithEmail(value, password)
        }

        val username = AuthUsernameNormalizer.normalizeForLoginLookup(value)
        if (username.length < MIN_USERNAME_LENGTH) {
            return AuthLoginResult.Failure(AuthLoginFailure.USERNAME_TOO_SHORT)
        }

        return when (val lookup = dataSource.lookupEmailForUsername(username)) {
            is UsernameEmailLookupResult.Found -> {
                dataSource.signInWithEmail(lookup.email, password)
            }

            UsernameEmailLookupResult.NotFound -> {
                AuthLoginResult.Failure(AuthLoginFailure.USERNAME_NOT_FOUND)
            }

            UsernameEmailLookupResult.MissingEmail -> {
                AuthLoginResult.Failure(AuthLoginFailure.USERNAME_PROFILE_MISSING)
            }

            is UsernameEmailLookupResult.Failure -> AuthLoginResult.Failure(lookup.reason)
        }
    }

    suspend fun sendPasswordReset(email: String): PasswordResetResult {
        return dataSource.sendPasswordReset(email.trim())
    }

    fun currentUserId(): String? = dataSource.currentSession()?.uid

    fun blockUnverifiedPasswordUserIfNeeded(): Boolean {
        val session = dataSource.currentSession() ?: return false
        if (!session.isPasswordUser || session.isEmailVerified) return false

        dataSource.sendCurrentUserVerificationEmail()
        dataSource.signOut()
        return true
    }

    private companion object {
        const val MIN_USERNAME_LENGTH = 3
    }
}
