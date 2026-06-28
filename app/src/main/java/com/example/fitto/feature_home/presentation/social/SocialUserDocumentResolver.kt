package com.example.fitto.feature_home.presentation.social

internal object SocialUserDocumentResolver {

    fun resolveProfile(
        id: String,
        userName: String?,
        username: String?,
        displayName: String?,
        name: String?,
        realName: String?,
        email: String?,
        photoUrl: String?,
        bio: String?,
        followersCount: Int,
        followingCount: Int,
        isFollowedByMe: Boolean = false
    ): SocialUser {
        return SocialUser(
            id = id,
            userName = userName.orEmpty()
                .ifBlank {
                    username.orEmpty()
                        .ifBlank {
                            email.orEmpty()
                                .substringBefore("@")
                                .ifBlank { "fitto_user" }
                        }
                },
            displayName = displayName
                ?: userName
                ?: username
                ?: name
                ?: realName,
            personalName = realName ?: name,
            email = email,
            photoUrl = photoUrl,
            bio = bio,
            followersCount = followersCount,
            followingCount = followingCount,
            isFollowedByMe = isFollowedByMe
        )
    }

    fun resolveBlockedUser(
        id: String,
        userName: String?,
        username: String?,
        displayName: String?,
        name: String?,
        realName: String?,
        email: String?,
        photoUrl: String?,
        bio: String?,
        followersCount: Int,
        followingCount: Int,
        fallbackName: String
    ): SocialUser {
        return SocialUser(
            id = id,
            userName = userName
                ?: username
                ?: displayName
                ?: fallbackName,
            displayName = displayName
                ?: userName
                ?: username
                ?: fallbackName,
            personalName = realName ?: name,
            email = email,
            photoUrl = photoUrl,
            bio = bio,
            followersCount = followersCount,
            followingCount = followingCount
        )
    }
}
