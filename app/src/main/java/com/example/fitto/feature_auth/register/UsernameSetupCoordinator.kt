package com.example.fitto.feature_auth.register

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsernameSetupCoordinator(
    private val dataSource: UsernameSetupDataSource,
    private val payloadFactory: UsernameProfilePayloadFactory = UsernameProfilePayloadFactory(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun currentIdentity(): UsernameSetupIdentity? = dataSource.currentIdentity()

    suspend fun checkAvailability(username: String): UsernameAvailability {
        return withContext(ioDispatcher) {
            dataSource.checkAvailability(username)
        }
    }

    suspend fun saveProfile(
        username: String,
        displayName: String,
        state: OnboardingState,
        dailyNeeds: DailyNeedsResult?
    ): UsernameProfileSaveResult {
        val identity = dataSource.currentIdentity()
            ?: return UsernameProfileSaveResult.Failure()
        val payload = payloadFactory.create(
            uid = identity.uid,
            email = identity.email,
            username = username,
            displayName = displayName,
            state = state,
            dailyNeeds = dailyNeeds,
            now = clock()
        )
        return withContext(ioDispatcher) {
            dataSource.saveProfile(
                UsernameProfileSaveRequest(
                    identity = identity,
                    username = username,
                    payload = payload
                )
            )
        }
    }
}
