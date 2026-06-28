package com.example.fitto.feature_home.presentation.social

internal object SocialUserListResolver {

    private const val USER_LOOKUP_CHUNK_SIZE = 10

    fun shouldSkipRelationshipLookup(userId: String): Boolean {
        return userId.isBlank()
    }

    fun followerIds(ids: List<String?>): List<String> {
        return uniqueNonBlankIds(ids)
    }

    fun followingIds(ids: List<String?>): List<String> {
        return uniqueNonBlankIds(ids)
    }

    fun lookupChunks(userIds: List<String>): List<List<String>> {
        return uniqueNonBlankIds(userIds)
            .chunked(USER_LOOKUP_CHUNK_SIZE)
    }

    fun resolveVisibleProfile(
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
    ): SocialUser? {
        if (isDeleted == true) return null

        return SocialUserDocumentResolver.resolveProfile(
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
            followingCount = followingCount?.toInt() ?: 0
        )
    }

    fun orderUsersByRequestedIds(
        requestedIds: List<String>,
        usersById: Map<String, SocialUser>
    ): List<SocialUser> {
        return requestedIds.mapNotNull { id -> usersById[id] }
    }

    private fun uniqueNonBlankIds(ids: List<String?>): List<String> {
        return ids
            .map { id -> id.orEmpty() }
            .filter { id -> id.isNotBlank() }
            .distinct()
    }
}
