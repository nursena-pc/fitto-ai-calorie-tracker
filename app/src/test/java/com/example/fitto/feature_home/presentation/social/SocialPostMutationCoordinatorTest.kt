package com.example.fitto.feature_home.presentation.social

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialPostMutationCoordinatorTest {

    @Test
    fun createPost_routesRequestToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)
        val request = SocialPostCreateRequest(
            content = "Bugünkü öğünüm",
            imageUri = null,
            mealInfo = "Kahvaltı",
            calories = 420,
            allowComments = true,
            visibility = "community",
            tags = listOf("kahvaltı"),
            isDraft = false
        )

        coordinator.createPost(request)

        assertEquals(request, commandSource.createdPostRequest)
    }

    @Test
    fun createRecipePost_routesRequestAndReturnsPostId() = runBlocking {
        val commandSource = FakeCommandSource(recipePostId = "post-42")
        val coordinator = createCoordinator(commandSource = commandSource)
        val request = SocialRecipePostCreateRequest(
            content = "Yeni tarif",
            recipeId = "recipe-1",
            recipeTitle = "Protein Kasesi",
            recipeImageResId = 12,
            recipeDuration = "20 dk",
            recipeServing = "2 kişilik",
            recipeCalories = 510,
            recipeCategory = "Ana yemek",
            tags = listOf("protein"),
            recipeSourceTag = "community",
            recipeDescription = "Pratik tarif",
            recipeIngredients = listOf("Yoğurt"),
            recipeSteps = listOf("Karıştır")
        )

        val postId = coordinator.createRecipePost(request)

        assertEquals("post-42", postId)
        assertEquals(request, commandSource.createdRecipePostRequest)
    }

    @Test
    fun updateDraftPost_routesRequestToCommandSource() = runBlocking {
        val commandSource = FakeCommandSource()
        val coordinator = createCoordinator(commandSource = commandSource)
        val request = SocialDraftPostUpdateRequest(
            draftId = "draft-1",
            content = "Güncellenen içerik",
            imageUri = null,
            existingImageUrl = "https://example.com/image.jpg",
            mealInfo = "Akşam",
            calories = 640,
            allowComments = false,
            visibility = "followers",
            tags = listOf("akşam"),
            publish = true
        )

        coordinator.updateDraftPost(request)

        assertEquals(request, commandSource.updatedDraftRequest)
    }

    @Test
    fun addComment_routesRequestToInteractionSource() = runBlocking {
        val interactionSource = FakeInteractionSource()
        val coordinator = createCoordinator(interactionSource = interactionSource)
        val request = SocialCommentCreateRequest(
            postId = "post-1",
            text = "Harika görünüyor",
            parentCommentId = "comment-parent"
        )

        coordinator.addComment(request)

        assertEquals(request, interactionSource.createdCommentRequest)
    }

    @Test
    fun toggleCommentLike_routesPostAndCommentIdsToInteractionSource() = runBlocking {
        val interactionSource = FakeInteractionSource()
        val coordinator = createCoordinator(interactionSource = interactionSource)

        coordinator.toggleCommentLike(
            postId = "post-1",
            commentId = "comment-1"
        )

        assertEquals("post-1", interactionSource.likedCommentPostId)
        assertEquals("comment-1", interactionSource.likedCommentId)
    }

    @Test
    fun postInteractions_routePostIdsToInteractionSource() = runBlocking {
        val interactionSource = FakeInteractionSource()
        val coordinator = createCoordinator(interactionSource = interactionSource)

        coordinator.toggleSave("saved-post")
        coordinator.toggleLike("liked-post")

        assertEquals("saved-post", interactionSource.savedPostId)
        assertEquals("liked-post", interactionSource.likedPostId)
    }

    private fun createCoordinator(
        commandSource: FakeCommandSource = FakeCommandSource(),
        interactionSource: FakeInteractionSource = FakeInteractionSource()
    ): SocialPostMutationCoordinator {
        return SocialPostMutationCoordinator(
            commandSource = commandSource,
            interactionSource = interactionSource
        )
    }

    private class FakeCommandSource(
        private val recipePostId: String? = null
    ) : SocialPostCommandSource {
        var createdPostRequest: SocialPostCreateRequest? = null
        var createdRecipePostRequest: SocialRecipePostCreateRequest? = null
        var updatedDraftRequest: SocialDraftPostUpdateRequest? = null

        override suspend fun createPost(request: SocialPostCreateRequest) {
            createdPostRequest = request
        }

        override suspend fun createRecipePost(request: SocialRecipePostCreateRequest): String? {
            createdRecipePostRequest = request
            return recipePostId
        }

        override suspend fun updateDraftPost(request: SocialDraftPostUpdateRequest) {
            updatedDraftRequest = request
        }
    }

    private class FakeInteractionSource : SocialPostInteractionSource {
        var createdCommentRequest: SocialCommentCreateRequest? = null
        var likedCommentPostId: String? = null
        var likedCommentId: String? = null
        var savedPostId: String? = null
        var likedPostId: String? = null

        override suspend fun addComment(request: SocialCommentCreateRequest) {
            createdCommentRequest = request
        }

        override suspend fun toggleCommentLike(postId: String, commentId: String) {
            likedCommentPostId = postId
            likedCommentId = commentId
        }

        override suspend fun toggleSave(postId: String) {
            savedPostId = postId
        }

        override suspend fun toggleLike(postId: String) {
            likedPostId = postId
        }
    }
}
