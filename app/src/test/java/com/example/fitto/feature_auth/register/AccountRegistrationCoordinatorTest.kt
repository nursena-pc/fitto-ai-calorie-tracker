package com.example.fitto.feature_auth.register

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRegistrationCoordinatorTest {

    @Test
    fun startRegistration_forwardsCredentials() = runBlocking {
        val source = FakeRegistrationDataSource()
        val coordinator = AccountRegistrationCoordinator(source)

        val result = coordinator.startRegistration("mail@example.com", "secret!")

        assertEquals(source.startResult, result)
        assertEquals("mail@example.com", source.email)
        assertEquals("secret!", source.password)
    }

    @Test
    fun verificationOperations_areForwarded() = runBlocking {
        val source = FakeRegistrationDataSource()
        val coordinator = AccountRegistrationCoordinator(source)

        assertTrue(coordinator.isEmailVerified())
        assertTrue(coordinator.resendVerificationEmail())
        coordinator.cancelRegistration()
        assertTrue(coordinator.createInitialProfile("uid", "mail@example.com"))

        assertTrue(source.cancelled)
        assertEquals("uid", source.profileUid)
    }

    private class FakeRegistrationDataSource : AccountRegistrationDataSource {
        val startResult = AccountRegistrationStartResult.VerificationSent(
            uid = "uid",
            email = "mail@example.com",
            wasResent = false
        )
        var email: String? = null
        var password: String? = null
        var cancelled = false
        var profileUid: String? = null

        override suspend fun startRegistration(
            email: String,
            password: String
        ): AccountRegistrationStartResult {
            this.email = email
            this.password = password
            return startResult
        }

        override suspend fun isCurrentUserEmailVerified(): Boolean = true

        override suspend fun resendCurrentUserVerificationEmail(): Boolean = true

        override suspend fun cancelUnverifiedRegistration() {
            cancelled = true
        }

        override suspend fun createInitialProfile(uid: String, email: String): Boolean {
            profileUid = uid
            return true
        }
    }
}
