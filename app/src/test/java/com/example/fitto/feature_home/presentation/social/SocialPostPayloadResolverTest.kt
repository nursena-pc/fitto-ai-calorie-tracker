package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialPostPayloadResolverTest {

    @Test
    fun resolveAuthor_prefersProfileNameAndPhoto() {
        val author = SocialPostPayloadResolver.resolveAuthor(
            profileUserName = "nursena",
            profilePhotoUrl = "profile-photo",
            email = "mail@example.com",
            authPhotoUrl = "auth-photo"
        )

        assertEquals("nursena", author.userName)
        assertEquals("profile-photo", author.photoUrl)
    }

    @Test
    fun resolveAuthor_fallsBackToEmailPrefixAndAuthPhoto() {
        val author = SocialPostPayloadResolver.resolveAuthor(
            profileUserName = "   ",
            profilePhotoUrl = null,
            email = "nursena@example.com",
            authPhotoUrl = "auth-photo"
        )

        assertEquals("nursena", author.userName)
        assertEquals("auth-photo", author.photoUrl)
    }

    @Test
    fun createPostPayload_trimsContentAndMealInfo() {
        val payload = SocialPostPayloadResolver.createPostPayload(
            userId = "user-1",
            author = SocialPostAuthor("nursena", "photo"),
            content = "  hello  ",
            imageUrl = "image",
            mealInfo = "  lunch  ",
            calories = 450,
            allowComments = true,
            visibility = "community",
            tags = listOf("fit"),
            isDraft = false,
            createdAt = 100L
        )

        assertEquals("user-1", payload["userId"])
        assertEquals("nursena", payload["userName"])
        assertEquals("photo", payload["userPhotoUrl"])
        assertEquals("photo", payload["photoUrl"])
        assertEquals("hello", payload["content"])
        assertEquals("lunch", payload["mealInfo"])
        assertEquals(450, payload["calories"])
        assertEquals("community", payload["visibility"])
        assertEquals(listOf("fit"), payload["tags"])
        assertEquals(false, payload["isDraft"])
        assertEquals(0, payload["likesCount"])
        assertEquals(0, payload["commentsCount"])
        assertEquals(emptyList<String>(), payload["likedBy"])
        assertEquals(emptyList<String>(), payload["savedBy"])
        assertEquals(true, payload["allowComments"])
        assertEquals(100L, payload["createdAt"])
    }

    @Test
    fun createRecipePostPayload_filtersTagsIngredientsAndSteps() {
        val payload = SocialPostPayloadResolver.createRecipePostPayload(
            userId = "user-1",
            author = SocialPostAuthor("nursena", null),
            content = "  recipe content  ",
            recipeId = "recipe-1",
            recipeTitle = "  Bowl  ",
            recipeImageResId = 12,
            recipeDuration = "20 dk",
            recipeServing = "2",
            recipeCalories = 500,
            recipeCategory = "Kahvaltı",
            tags = listOf("fit", "", "fit", "quick"),
            recipeSourceTag = "source",
            recipeDescription = "  desc  ",
            recipeIngredients = listOf(" egg ", "", " milk "),
            recipeSteps = listOf(" mix ", " ", "cook"),
            createdAt = 200L
        )

        assertEquals("recipe", payload["postType"])
        assertEquals("community", payload["visibility"])
        assertEquals("Bowl", payload["mealInfo"])
        assertEquals("recipe content", payload["content"])
        assertEquals(listOf("fit", "quick"), payload["tags"])
        assertEquals("desc", payload["recipeDescription"])
        assertEquals(listOf("egg", "milk"), payload["recipeIngredients"])
        assertEquals(listOf("mix", "cook"), payload["recipeSteps"])
        assertNull(payload["imageUrl"])
    }

    @Test
    fun createCommentPayload_trimsTextAndKeepsParentComment() {
        val payload = SocialPostPayloadResolver.createCommentPayload(
            userId = "user-1",
            author = SocialPostAuthor("nursena", "photo"),
            postId = "post-1",
            postOwnerId = "owner-1",
            text = "  yorum  ",
            parentCommentId = "parent-1",
            createdAt = 300L
        )

        assertEquals("user-1", payload["userId"])
        assertEquals("nursena", payload["userName"])
        assertEquals("photo", payload["userPhotoUrl"])
        assertEquals("photo", payload["photoUrl"])
        assertEquals("post-1", payload["postId"])
        assertEquals("owner-1", payload["postOwnerId"])
        assertEquals("yorum", payload["text"])
        assertEquals("parent-1", payload["parentCommentId"])
        assertEquals(0, payload["likesCount"])
        assertEquals(0, payload["replyCount"])
        assertEquals(emptyList<String>(), payload["likedBy"])
        assertEquals(300L, payload["createdAt"])
    }
}
