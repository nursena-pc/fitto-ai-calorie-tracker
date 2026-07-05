package com.example.fitto.feature_auth.register

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class UsernameSetupCoordinatorTest {

    @Test
    fun checkAvailability_forwardsUsername() = runBlocking {
        val source = FakeUsernameSetupDataSource()
        val coordinator = coordinator(source)

        val result = coordinator.checkAvailability("fitto.user")

        assertEquals(UsernameAvailability.AVAILABLE, result)
        assertEquals("fitto.user", source.checkedUsername)
    }

    @Test
    fun saveProfile_buildsPayloadAndForwardsRequest() = runBlocking {
        val source = FakeUsernameSetupDataSource()
        val coordinator = coordinator(source)

        val result = coordinator.saveProfile(
            username = "fitto.user",
            displayName = "Fitto User",
            state = OnboardingState(name = "Nursena", weight = 70),
            dailyNeeds = null
        )

        assertSame(source.saveResult, result)
        val request = requireNotNull(source.savedRequest)
        assertEquals("uid-1", request.identity.uid)
        assertEquals("fitto.user", request.username)
        assertEquals("Nursena", request.payload.userDocument["name"])
        assertEquals(999L, request.payload.userDocument["updatedAt"])
    }

    @Test
    fun saveProfile_withoutIdentity_returnsFailure() = runBlocking {
        val source = FakeUsernameSetupDataSource(identity = null)
        val coordinator = coordinator(source)

        val result = coordinator.saveProfile(
            username = "fitto.user",
            displayName = "Fitto User",
            state = OnboardingState(),
            dailyNeeds = null
        )

        assertEquals(UsernameProfileSaveResult.Failure(), result)
        assertEquals(null, source.savedRequest)
    }

    private fun coordinator(source: UsernameSetupDataSource): UsernameSetupCoordinator {
        return UsernameSetupCoordinator(
            dataSource = source,
            clock = { 999L },
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    private class FakeUsernameSetupDataSource(
        private val identity: UsernameSetupIdentity? = UsernameSetupIdentity(
            uid = "uid-1",
            email = "user@example.com",
            displayName = "Fitto User"
        )
    ) : UsernameSetupDataSource {
        val saveResult = UsernameProfileSaveResult.Success(emptyMap())
        var checkedUsername: String? = null
        var savedRequest: UsernameProfileSaveRequest? = null

        override fun currentIdentity(): UsernameSetupIdentity? = identity

        override suspend fun checkAvailability(username: String): UsernameAvailability {
            checkedUsername = username
            return UsernameAvailability.AVAILABLE
        }

        override suspend fun saveProfile(
            request: UsernameProfileSaveRequest
        ): UsernameProfileSaveResult {
            savedRequest = request
            return saveResult
        }
    }
}
