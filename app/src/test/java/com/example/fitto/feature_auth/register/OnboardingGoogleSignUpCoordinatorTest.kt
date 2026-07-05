package com.example.fitto.feature_auth.register

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGoogleSignUpCoordinatorTest {

    @Test
    fun completedExistingProfile_opensHome() = runBlocking {
        val source = FakeSource(profileExists = true, profileCompleted = true)

        val result = OnboardingGoogleSignUpCoordinator(source).signIn("token")

        assertEquals(
            GoogleOnboardingResult.Success(GoogleOnboardingDestination.HOME),
            result
        )
        assertEquals(0, source.createCalls)
    }

    @Test
    fun incompleteExistingProfile_opensUsernameSetup() = runBlocking {
        val source = FakeSource(profileExists = true, profileCompleted = false)

        val result = OnboardingGoogleSignUpCoordinator(source).signIn("token")

        assertEquals(
            GoogleOnboardingResult.Success(GoogleOnboardingDestination.USERNAME_SETUP),
            result
        )
        assertEquals(0, source.createCalls)
    }

    @Test
    fun newProfile_isCreatedBeforeUsernameSetup() = runBlocking {
        val source = FakeSource(profileExists = false, profileCompleted = false)

        val result = OnboardingGoogleSignUpCoordinator(source).signIn("token")

        assertEquals(
            GoogleOnboardingResult.Success(GoogleOnboardingDestination.USERNAME_SETUP),
            result
        )
        assertEquals(1, source.createCalls)
        assertEquals(source.identity, source.createdIdentity)
    }

    @Test
    fun dataSourceFailure_returnsFailure() = runBlocking {
        val source = FakeSource(
            profileExists = false,
            profileCompleted = false,
            failure = IllegalStateException("boom")
        )

        val result = OnboardingGoogleSignUpCoordinator(source).signIn("token")

        assertTrue(result is GoogleOnboardingResult.Failure)
    }

    @Test
    fun blankToken_isRejectedWithoutCallingDataSource() = runBlocking {
        val source = FakeSource(profileExists = false, profileCompleted = false)

        val result = OnboardingGoogleSignUpCoordinator(source).signIn(" ")

        assertTrue(result is GoogleOnboardingResult.Failure)
        assertEquals(0, source.signInCalls)
    }

    private class FakeSource(
        private val profileExists: Boolean,
        private val profileCompleted: Boolean,
        private val failure: Throwable? = null
    ) : GoogleOnboardingDataSource {
        val identity = GoogleOnboardingIdentity(
            uid = "uid-1",
            email = "user@example.com",
            displayName = "User",
            photoUrl = null
        )
        var signInCalls = 0
        var createCalls = 0
        var createdIdentity: GoogleOnboardingIdentity? = null

        override suspend fun signIn(idToken: String): GoogleOnboardingIdentity? {
            signInCalls += 1
            failure?.let { throw it }
            return identity
        }

        override suspend fun profileStatus(uid: String): GoogleOnboardingProfileStatus {
            return GoogleOnboardingProfileStatus(
                exists = profileExists,
                isCompleted = profileCompleted
            )
        }

        override suspend fun createInitialProfile(identity: GoogleOnboardingIdentity) {
            createCalls += 1
            createdIdentity = identity
        }
    }
}
