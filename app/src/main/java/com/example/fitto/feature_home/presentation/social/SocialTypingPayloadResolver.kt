package com.example.fitto.feature_home.presentation.social

internal object SocialTypingPayloadResolver {

    fun createPayload(
        uid: String,
        profileUserName: String?,
        profileUsername: String?,
        profilePhotoUrl: String?,
        email: String?,
        authPhotoUrl: String?,
        updatedAt: Any
    ): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "userName" to resolveUserName(
                profileUserName = profileUserName,
                profileUsername = profileUsername,
                email = email
            ),
            "photoUrl" to resolvePhotoUrl(
                profilePhotoUrl = profilePhotoUrl,
                authPhotoUrl = authPhotoUrl
            ),
            "isTyping" to true,
            "updatedAt" to updatedAt
        )
    }

    private fun resolveUserName(
        profileUserName: String?,
        profileUsername: String?,
        email: String?
    ): String {
        return profileUserName?.ifBlank { null }
            ?: profileUsername?.ifBlank { null }
            ?: email?.substringBefore("@")?.ifBlank { null }
            ?: "fitto_user"
    }

    private fun resolvePhotoUrl(
        profilePhotoUrl: String?,
        authPhotoUrl: String?
    ): String? {
        return profilePhotoUrl?.ifBlank { null }
            ?: authPhotoUrl?.ifBlank { null }
    }
}
