package com.example.fitto.feature_home.presentation.social

internal object SocialUserLookupResolver {

    fun normalizeSearchQuery(query: String): String {
        return SocialTextResolver.normalizeUsername(query)
    }

    fun shouldSearch(normalizedQuery: String): Boolean {
        return normalizedQuery.length >= MIN_SEARCH_QUERY_LENGTH
    }

    fun uniqueNonBlankIds(ids: List<String>): List<String> {
        return ids.filter { it.isNotBlank() }.distinct()
    }

    fun orderUsersByRequestedIds(
        requestedIds: List<String>,
        usersById: Map<String, SocialUser>
    ): List<SocialUser> {
        return requestedIds.mapNotNull { id -> usersById[id] }
    }

    fun resolveSearchUser(
        id: String,
        currentUserId: String,
        isDeleted: Boolean,
        userName: String?,
        usernameLowercase: String?,
        email: String?,
        bio: String?,
        photoUrl: String?
    ): SearchUserUiModel? {
        if (id.isBlank() || id == currentUserId || isDeleted) return null

        val resolvedUserName = userName?.ifBlank { null }
            ?: usernameLowercase?.ifBlank { null }
            ?: email?.substringBefore("@")?.ifBlank { null }
            ?: return null

        return SearchUserUiModel(
            id = id,
            userName = resolvedUserName,
            bio = bio,
            photoUrl = photoUrl
        )
    }

    private const val MIN_SEARCH_QUERY_LENGTH = 2
}
