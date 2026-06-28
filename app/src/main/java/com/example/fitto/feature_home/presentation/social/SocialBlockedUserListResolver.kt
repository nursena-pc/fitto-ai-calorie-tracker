package com.example.fitto.feature_home.presentation.social

internal object SocialBlockedUserListResolver {

    private const val USER_LOOKUP_CHUNK_SIZE = 10
    private const val DEFAULT_USER_NAME = "fitto_user"
    private const val DELETED_USER_NAME = "Silinen Kullanıcı"

    fun blockedUserIds(
        ids: List<String>,
        currentUserId: String
    ): List<String> {
        return ids
            .filter { userId -> userId.isNotBlank() && userId != currentUserId }
            .distinct()
    }

    fun lookupChunks(blockedUserIds: List<String>): List<List<String>> {
        return blockedUserIds.chunked(USER_LOOKUP_CHUNK_SIZE)
    }

    fun resolveBlockedUser(
        id: String,
        isDeleted: Boolean?,
        userName: String?,
        username: String?,
        displayName: String?,
        name: String?,
        realName: String?,
        email: String?,
        photoUrl: String?,
        bio: String?,
        followersCount: Long?,
        followingCount: Long?
    ): SocialUser {
        return SocialUserDocumentResolver.resolveBlockedUser(
            id = id,
            userName = userName,
            username = username,
            displayName = displayName,
            name = name,
            realName = realName,
            email = email,
            photoUrl = photoUrl,
            bio = bio,
            followersCount = followersCount?.toInt() ?: 0,
            followingCount = followingCount?.toInt() ?: 0,
            fallbackName = fallbackName(isDeleted)
        )
    }

    fun orderBlockedUsers(
        blockedUserIds: List<String>,
        usersById: Map<String, SocialUser>
    ): List<SocialUser> {
        return blockedUserIds
            .map { userId -> usersById[userId] ?: deletedUser(userId) }
            .sortedBy { user -> user.userName.ifBlank { user.displayName.orEmpty() } }
    }

    private fun fallbackName(isDeleted: Boolean?): String {
        return if (isDeleted == true) DELETED_USER_NAME else DEFAULT_USER_NAME
    }

    private fun deletedUser(userId: String): SocialUser {
        return SocialUser(
            id = userId,
            userName = DELETED_USER_NAME,
            displayName = DELETED_USER_NAME
        )
    }
}
