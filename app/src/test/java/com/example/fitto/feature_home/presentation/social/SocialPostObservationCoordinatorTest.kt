package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialPostObservationCoordinatorTest {

    private val post = SocialPost(
        id = "post-1",
        userId = "user-1",
        userName = "Nursena"
    )
    private val comment = SocialComment(
        id = "comment-1",
        postId = "post-1",
        userId = "user-1",
        userName = "Nursena",
        text = "Merhaba"
    )

    @Test
    fun observeFeed_routesToFeedSource() = runBlocking {
        val feedSource = FakeFeedSource(posts = listOf(post))
        val coordinator = createCoordinator(feedSource = feedSource)

        assertEquals(listOf(post), coordinator.observeFeed().first())
        assertEquals(1, feedSource.observationCount)
    }

    @Test
    fun refreshFeedFromServer_routesToFeedSource() = runBlocking {
        val feedSource = FakeFeedSource()
        val coordinator = createCoordinator(feedSource = feedSource)

        coordinator.refreshFeedFromServer()

        assertEquals(1, feedSource.refreshCount)
    }

    @Test
    fun observeComments_routesPostToContentSource() = runBlocking {
        val contentSource = FakeContentSource(comments = listOf(comment))
        val coordinator = createCoordinator(contentSource = contentSource)

        assertEquals(listOf(comment), coordinator.observeComments("post-1").first())
        assertEquals("post-1", contentSource.observedCommentsPostId)
    }

    @Test
    fun profileDraftAndSavedObservations_routeToContentSource() = runBlocking {
        val contentSource = FakeContentSource(
            profilePosts = listOf(post),
            draftPosts = listOf(post),
            savedPosts = listOf(post)
        )
        val coordinator = createCoordinator(contentSource = contentSource)

        assertEquals(listOf(post), coordinator.observeMyProfilePosts().first())
        assertEquals(listOf(post), coordinator.observeMyDraftPosts().first())
        assertEquals(listOf(post), coordinator.observeSavedPosts().first())
        assertEquals(1, contentSource.profileObservationCount)
        assertEquals(1, contentSource.draftObservationCount)
        assertEquals(1, contentSource.savedObservationCount)
    }

    @Test
    fun observePost_routesPostToContentSource() = runBlocking {
        val contentSource = FakeContentSource(observedPost = post)
        val coordinator = createCoordinator(contentSource = contentSource)

        assertEquals(post, coordinator.observePost("post-1").first())
        assertEquals("post-1", contentSource.observedPostId)
    }

    @Test
    fun getPostById_routesPostToContentSource() = runBlocking {
        val contentSource = FakeContentSource(postById = post)
        val coordinator = createCoordinator(contentSource = contentSource)

        assertEquals(post, coordinator.getPostById("post-1"))
        assertEquals("post-1", contentSource.requestedPostId)
    }

    private fun createCoordinator(
        feedSource: FakeFeedSource = FakeFeedSource(),
        contentSource: FakeContentSource = FakeContentSource()
    ): SocialPostObservationCoordinator {
        return SocialPostObservationCoordinator(
            feedSource = feedSource,
            contentSource = contentSource
        )
    }

    private class FakeFeedSource(
        private val posts: List<SocialPost> = emptyList()
    ) : SocialFeedObservationSource {
        var observationCount: Int = 0
        var refreshCount: Int = 0

        override fun observeFeed(): Flow<List<SocialPost>> {
            observationCount += 1
            return flowOf(posts)
        }

        override suspend fun refreshFeedFromServer() {
            refreshCount += 1
        }
    }

    private class FakeContentSource(
        private val comments: List<SocialComment> = emptyList(),
        private val profilePosts: List<SocialPost> = emptyList(),
        private val draftPosts: List<SocialPost> = emptyList(),
        private val savedPosts: List<SocialPost> = emptyList(),
        private val observedPost: SocialPost? = null,
        private val postById: SocialPost? = null
    ) : SocialPostContentObservationSource {
        var observedCommentsPostId: String? = null
        var profileObservationCount: Int = 0
        var draftObservationCount: Int = 0
        var savedObservationCount: Int = 0
        var observedPostId: String? = null
        var requestedPostId: String? = null

        override fun observeComments(postId: String): Flow<List<SocialComment>> {
            observedCommentsPostId = postId
            return flowOf(comments)
        }

        override fun observeMyProfilePosts(): Flow<List<SocialPost>> {
            profileObservationCount += 1
            return flowOf(profilePosts)
        }

        override fun observeMyDraftPosts(): Flow<List<SocialPost>> {
            draftObservationCount += 1
            return flowOf(draftPosts)
        }

        override fun observeSavedPosts(): Flow<List<SocialPost>> {
            savedObservationCount += 1
            return flowOf(savedPosts)
        }

        override fun observePost(postId: String): Flow<SocialPost?> {
            observedPostId = postId
            return flowOf(observedPost)
        }

        override suspend fun getPostById(postId: String): SocialPost? {
            requestedPostId = postId
            return postById
        }
    }
}
