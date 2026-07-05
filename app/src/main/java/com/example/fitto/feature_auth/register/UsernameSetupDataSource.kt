package com.example.fitto.feature_auth.register

data class UsernameSetupIdentity(
    val uid: String,
    val email: String,
    val displayName: String?
)

enum class UsernameAvailability {
    AVAILABLE,
    TAKEN
}

data class UsernameProfileSaveRequest(
    val identity: UsernameSetupIdentity,
    val username: String,
    val payload: UsernameProfilePayload
)

sealed interface UsernameProfileSaveResult {
    data class Success(val userDocument: Map<String, Any?>) : UsernameProfileSaveResult
    data object UsernameTaken : UsernameProfileSaveResult
    data class Failure(val cause: Throwable? = null) : UsernameProfileSaveResult
}

interface UsernameSetupDataSource {
    fun currentIdentity(): UsernameSetupIdentity?
    suspend fun checkAvailability(username: String): UsernameAvailability
    suspend fun saveProfile(request: UsernameProfileSaveRequest): UsernameProfileSaveResult
}
