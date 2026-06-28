package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialPostDocumentResolverTest {

    @Test
    fun resolve_returnsNullWhenUserIdIsBlank() {
        assertNull(
            SocialPostDocumentResolver.resolve(
                id = "post-1",
                currentUserId = "current-user",
                userId = "   ",
                userName = "Nur",
                userPhotoUrl = null,
                fallbackPhotoUrl = null,
                content = null,
                imageUrl = null,
                mealInfo = null,
                calories = null,
                likesCount = null,
                commentsCount = null,
                createdAt = null,
                likedBy = null,
                savedBy = null,
                allowComments = null,
                visibility = null,
                tags = null,
                isDraft = null,
                postType = null,
                recipeId = null,
                recipeTitle = null,
                recipeImageResId = null,
                recipeDuration = null,
                recipeServing = null,
                recipeCalories = null,
                recipeCategory = null,
                recipeSourceTag = null,
                recipeDescription = null,
                recipeIngredients = null,
                recipeSteps = null
            )
        )
    }

    @Test
    fun resolve_mapsDefaultsAndCurrentUserState() {
        val post = SocialPostDocumentResolver.resolve(
            id = "post-1",
            currentUserId = "current-user",
            userId = "current-user",
            userName = "",
            userPhotoUrl = null,
            fallbackPhotoUrl = "fallback.jpg",
            content = null,
            imageUrl = "image.jpg",
            mealInfo = "Kahvaltı",
            calories = 410L,
            likesCount = 3L,
            commentsCount = 2L,
            createdAt = 123L,
            likedBy = listOf("current-user", 42, "other-user"),
            savedBy = listOf("current-user"),
            allowComments = null,
            visibility = "",
            tags = listOf("fit", 7, "food"),
            isDraft = null,
            postType = "recipe",
            recipeId = "recipe-1",
            recipeTitle = "Omlet",
            recipeImageResId = 99L,
            recipeDuration = "10 dk",
            recipeServing = "1 kişi",
            recipeCalories = 410L,
            recipeCategory = "Kahvaltı",
            recipeSourceTag = "Fitto",
            recipeDescription = "Proteinli",
            recipeIngredients = listOf("Yumurta", 1, "Peynir"),
            recipeSteps = listOf("Karıştır", 2, "Pişir")
        )!!

        assertEquals("post-1", post.id)
        assertEquals("current-user", post.userId)
        assertEquals("fitto_user", post.userName)
        assertEquals("fallback.jpg", post.userPhotoUrl)
        assertEquals("", post.content)
        assertEquals("image.jpg", post.imageUrl)
        assertEquals("Kahvaltı", post.mealInfo)
        assertEquals(410, post.calories)
        assertEquals(3, post.likesCount)
        assertEquals(2, post.commentsCount)
        assertEquals(123L, post.createdAt)
        assertTrue(post.likedByMe)
        assertTrue(post.savedByMe)
        assertTrue(post.allowComments)
        assertEquals(POST_VISIBILITY_COMMUNITY, post.visibility)
        assertEquals(listOf("fit", "food"), post.tags)
        assertFalse(post.isDraft)
        assertTrue(post.isMine)
        assertEquals("recipe", post.postType)
        assertEquals("recipe-1", post.recipeId)
        assertEquals("Omlet", post.recipeTitle)
        assertEquals(99, post.recipeImageResId)
        assertEquals("10 dk", post.recipeDuration)
        assertEquals("1 kişi", post.recipeServing)
        assertEquals(410, post.recipeCalories)
        assertEquals("Kahvaltı", post.recipeCategory)
        assertEquals("Fitto", post.recipeSourceTag)
        assertEquals("Proteinli", post.recipeDescription)
        assertEquals(listOf("Yumurta", "Peynir"), post.recipeIngredients)
        assertEquals(listOf("Karıştır", "Pişir"), post.recipeSteps)
    }

    @Test
    fun resolve_preservesExplicitPostValues() {
        val post = SocialPostDocumentResolver.resolve(
            id = "post-2",
            currentUserId = "current-user",
            userId = "other-user",
            userName = "Ayşe",
            userPhotoUrl = "profile.jpg",
            fallbackPhotoUrl = "fallback.jpg",
            content = "Merhaba",
            imageUrl = null,
            mealInfo = null,
            calories = null,
            likesCount = null,
            commentsCount = null,
            createdAt = null,
            likedBy = emptyList<String>(),
            savedBy = emptyList<String>(),
            allowComments = false,
            visibility = POST_VISIBILITY_FOLLOWERS,
            tags = emptyList<String>(),
            isDraft = true,
            postType = null,
            recipeId = null,
            recipeTitle = null,
            recipeImageResId = null,
            recipeDuration = null,
            recipeServing = null,
            recipeCalories = null,
            recipeCategory = null,
            recipeSourceTag = null,
            recipeDescription = null,
            recipeIngredients = emptyList<String>(),
            recipeSteps = emptyList<String>()
        )!!

        assertEquals("Ayşe", post.userName)
        assertEquals("profile.jpg", post.userPhotoUrl)
        assertEquals("Merhaba", post.content)
        assertFalse(post.allowComments)
        assertEquals(POST_VISIBILITY_FOLLOWERS, post.visibility)
        assertTrue(post.isDraft)
        assertFalse(post.isMine)
        assertFalse(post.likedByMe)
        assertFalse(post.savedByMe)
    }
}
