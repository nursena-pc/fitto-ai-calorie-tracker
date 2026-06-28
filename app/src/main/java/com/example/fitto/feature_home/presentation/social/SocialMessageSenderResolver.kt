package com.example.fitto.feature_home.presentation.social

internal data class SocialMessageSender(
    val name: String,
    val photoUrl: String?
)

internal object SocialMessageSenderResolver {

    fun resolve(
        profileUserName: String?,
        profilePhotoUrl: String?,
        currentUserEmail: String?,
        currentUserPhotoUrl: String?
    ): SocialMessageSender {
        val senderName = profileUserName?.ifBlank { null }
            ?: currentUserEmail?.substringBefore("@")
            ?: "fitto_user"

        return SocialMessageSender(
            name = senderName,
            photoUrl = profilePhotoUrl ?: currentUserPhotoUrl
        )
    }
}
