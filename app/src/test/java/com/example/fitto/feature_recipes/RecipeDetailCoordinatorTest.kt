package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeDetailCoordinatorTest {

    @Test
    fun loadRecipe_forwardsIdAndReturnsSourceRecipe() = runBlocking {
        val source = FakeRecipeDetailDataSource(recipe = recipe())
        val coordinator = coordinator(source)

        val result = coordinator.loadRecipe("recipe-1")

        assertEquals("recipe-1", source.loadedRecipeId)
        assertEquals("recipe-1", result?.id)
    }

    @Test
    fun loadRecipe_blankIdDoesNotCallSource() = runBlocking {
        val source = FakeRecipeDetailDataSource(recipe = recipe())
        val coordinator = coordinator(source)

        val result = coordinator.loadRecipe("   ")

        assertNull(result)
        assertNull(source.loadedRecipeId)
    }

    @Test
    fun saveOriginalRecipe_forwardsRecipeId() = runBlocking {
        val source = FakeRecipeDetailDataSource()
        val coordinator = coordinator(source)

        coordinator.saveOriginalRecipe("recipe-1")

        assertEquals("recipe-1", source.savedFavoriteId)
    }

    @Test
    fun saveCommunityRecipe_forwardsTypedRequestAndResult() = runBlocking {
        val request = communityBookRequest()
        val source = FakeRecipeDetailDataSource(communitySaveResult = "saved-1")
        val coordinator = coordinator(source)

        val result = coordinator.saveCommunityRecipe(request)

        assertEquals(request, source.communityBookRequest)
        assertEquals("saved-1", result)
    }

    @Test
    fun shareRecipeToCommunity_forwardsTypedRequestAndResult() = runBlocking {
        val request = RecipeCommunityShareRequest(
            recipeId = "recipe-1",
            ingredients = listOf("1 adet Havuç"),
            comment = "Deneyin"
        )
        val source = FakeRecipeDetailDataSource(communityShareResult = "post-1")
        val coordinator = coordinator(source)

        val result = coordinator.shareRecipeToCommunity(request)

        assertEquals(request, source.communityShareRequest)
        assertEquals("post-1", result)
    }

    @Test
    fun copyRecipeToUserBook_forwardsTypedRequestAndResult() = runBlocking {
        val request = RecipeBookCopyRequest(
            recipeId = "recipe-1",
            ingredientsOverride = listOf("2 adet Havuç")
        )
        val source = FakeRecipeDetailDataSource(copyResult = "copy-1")
        val coordinator = coordinator(source)

        val result = coordinator.copyRecipeToUserBook(request)

        assertEquals(request, source.bookCopyRequest)
        assertEquals("copy-1", result)
    }

    private fun coordinator(source: RecipeDetailDataSource): RecipeDetailCoordinator {
        return RecipeDetailCoordinator(
            dataSource = source,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    private fun communityBookRequest(): RecipeCommunityBookRequest {
        return RecipeCommunityBookRequest(
            postId = "post-1",
            postUserId = "user-1",
            recipeId = "recipe-1",
            recipeTitle = "Test Tarifi",
            recipeImageResId = 10,
            recipeDuration = "30 dk",
            recipeServing = "2 kişilik",
            recipeCalories = 300,
            recipeCategory = "main",
            recipeSourceTag = "community_recipe",
            recipeDescription = "Açıklama",
            recipeIngredients = listOf("1 adet Havuç"),
            recipeSteps = listOf("Karıştır")
        )
    }

    private fun recipe(): RecipeUiModel {
        return RecipeUiModel(
            id = "recipe-1",
            title = "Test Tarifi",
            description = "Açıklama",
            mainCategoryKey = "main",
            subcategoryKey = "sub",
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = 30,
            durationLabel = "30 dk",
            difficultyKey = "easy",
            calories = 300,
            imageResId = 0,
            ingredients = listOf("1 adet havuç"),
            steps = listOf("Karıştır")
        )
    }

    private class FakeRecipeDetailDataSource(
        private val recipe: RecipeUiModel? = null,
        private val communitySaveResult: String? = null,
        private val communityShareResult: String? = null,
        private val copyResult: String? = null
    ) : RecipeDetailDataSource {

        var loadedRecipeId: String? = null
        var savedFavoriteId: String? = null
        var communityBookRequest: RecipeCommunityBookRequest? = null
        var communityShareRequest: RecipeCommunityShareRequest? = null
        var bookCopyRequest: RecipeBookCopyRequest? = null

        override fun findById(recipeId: String): RecipeUiModel? {
            loadedRecipeId = recipeId
            return recipe
        }

        override fun saveFavorite(recipeId: String) {
            savedFavoriteId = recipeId
        }

        override fun saveCommunityRecipe(request: RecipeCommunityBookRequest): String? {
            communityBookRequest = request
            return communitySaveResult
        }

        override suspend fun shareRecipeToCommunity(request: RecipeCommunityShareRequest): String? {
            communityShareRequest = request
            return communityShareResult
        }

        override fun copyRecipeToUserBook(request: RecipeBookCopyRequest): String? {
            bookCopyRequest = request
            return copyResult
        }
    }
}
