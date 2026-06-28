package com.example.fitto.feature_home.presentation.social

internal object SocialPostListResolver {

    fun profilePosts(posts: List<SocialPost>): List<SocialPost> {
        return publishedPosts(posts)
    }

    fun savedPosts(posts: List<SocialPost>): List<SocialPost> {
        return publishedPosts(posts)
    }

    fun draftPosts(posts: List<SocialPost>): List<SocialPost> {
        return posts
            .filter { post -> post.isDraft }
            .sortedByDescending { post -> post.createdAt }
    }

    fun visiblePost(
        post: SocialPost?,
        currentUserId: String
    ): SocialPost? {
        return post?.takeIf { candidate ->
            !candidate.isDraft || candidate.userId == currentUserId
        }
    }

    private fun publishedPosts(posts: List<SocialPost>): List<SocialPost> {
        return posts
            .filterNot { post -> post.isDraft }
            .sortedByDescending { post -> post.createdAt }
    }
}
