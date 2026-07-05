package com.example.fitto.feature_auth.login

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLoginCoordinatorTest {

    @Test
    fun signIn_withEmail_forwardsTrimmedEmail() = runBlocking {
        val source = FakeAuthLoginDataSource()
        val coordinator = AuthLoginCoordinator(source)

        val result = coordinator.signIn("  user@example.com  ", "secret")

        assertEquals(AuthLoginResult.Success, result)
        assertEquals("user@example.com", source.signedInEmail)
        assertEquals("secret", source.signedInPassword)
    }

    @Test
    fun signIn_withUsername_normalizesAndUsesResolvedEmail() = runBlocking {
        val source = FakeAuthLoginDataSource(
            lookupResult = UsernameEmailLookupResult.Found("mail@example.com")
        )
        val coordinator = AuthLoginCoordinator(source)

        val result = coordinator.signIn(" Çağrı User ", "secret")

        assertEquals(AuthLoginResult.Success, result)
        assertEquals("cagri.user", source.lookedUpUsername)
        assertEquals("mail@example.com", source.signedInEmail)
    }

    @Test
    fun signIn_withMissingUsername_returnsNotFound() = runBlocking {
        val coordinator = AuthLoginCoordinator(
            FakeAuthLoginDataSource(UsernameEmailLookupResult.NotFound)
        )

        val result = coordinator.signIn("missing", "secret")

        assertEquals(
            AuthLoginResult.Failure(AuthLoginFailure.USERNAME_NOT_FOUND),
            result
        )
    }

    @Test
    fun signIn_withShortUsername_rejectsBeforeLookup() = runBlocking {
        val source = FakeAuthLoginDataSource()
        val coordinator = AuthLoginCoordinator(source)

        val result = coordinator.signIn("ab", "secret")

        assertEquals(
            AuthLoginResult.Failure(AuthLoginFailure.USERNAME_TOO_SHORT),
            result
        )
        assertEquals(null, source.lookedUpUsername)
    }

    @Test
    fun blockUnverifiedPasswordUser_sendsVerificationAndSignsOut() {
        val source = FakeAuthLoginDataSource(
            session = AuthSessionSnapshot(
                uid = "uid-1",
                isPasswordUser = true,
                isEmailVerified = false
            )
        )
        val coordinator = AuthLoginCoordinator(source)

        assertTrue(coordinator.blockUnverifiedPasswordUserIfNeeded())
        assertTrue(source.verificationSent)
        assertTrue(source.signedOut)
    }

    @Test
    fun blockVerifiedUser_doesNothing() {
        val source = FakeAuthLoginDataSource(
            session = AuthSessionSnapshot(
                uid = "uid-1",
                isPasswordUser = true,
                isEmailVerified = true
            )
        )
        val coordinator = AuthLoginCoordinator(source)

        assertFalse(coordinator.blockUnverifiedPasswordUserIfNeeded())
        assertFalse(source.verificationSent)
        assertFalse(source.signedOut)
    }

    private class FakeAuthLoginDataSource(
        private val lookupResult: UsernameEmailLookupResult =
            UsernameEmailLookupResult.Found("resolved@example.com"),
        private val session: AuthSessionSnapshot? = null
    ) : AuthLoginDataSource {
        var lookedUpUsername: String? = null
        var signedInEmail: String? = null
        var signedInPassword: String? = null
        var verificationSent = false
        var signedOut = false

        override suspend fun lookupEmailForUsername(
            normalizedUsername: String
        ): UsernameEmailLookupResult {
            lookedUpUsername = normalizedUsername
            return lookupResult
        }

        override suspend fun signInWithEmail(
            email: String,
            password: String
        ): AuthLoginResult {
            signedInEmail = email
            signedInPassword = password
            return AuthLoginResult.Success
        }

        override suspend fun sendPasswordReset(email: String): PasswordResetResult {
            return PasswordResetResult.Success
        }

        override fun currentSession(): AuthSessionSnapshot? = session

        override fun sendCurrentUserVerificationEmail() {
            verificationSent = true
        }

        override fun signOut() {
            signedOut = true
        }
    }
}
