package com.example.fitto.feature_home.presentation.social

internal object SocialFeedResolver {

    private const val FirestoreWhereInLimit = 10

    fun shouldSkipFeed(currentUserId: String): Boolean {
        return currentUserId.isBlank()
    }

    fun followingIds(rawIds: Iterable<String?>): Set<String> {
        return rawIds
            .mapNotNull { id -> id?.trim()?.takeIf { it.isNotBlank() } }
            .toSet()
    }

    fun followingLookupChunks(
        followingIds: Set<String>,
        chunkSize: Int = FirestoreWhereInLimit
    ): List<List<String>> {
        if (chunkSize <= 0) return emptyList()
        return followingIds.toList().chunked(chunkSize)
    }

    fun sourcePostMap(posts: List<SocialPost>): Map<String, SocialPost> {
        return posts.associateBy { post -> post.id }
    }

    fun mergeSourcePosts(sourcePostMaps: Map<String, Map<String, SocialPost>>): List<SocialPost> {
        return sourcePostMaps
            .values
            .flatMap { postsById -> postsById.values }
            .associateBy { post -> post.id }
            .values
            .sortedByDescending { post -> post.createdAt }
    }
}
