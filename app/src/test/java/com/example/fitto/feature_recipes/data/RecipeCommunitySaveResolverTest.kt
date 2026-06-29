package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeCommunitySaveResolverTest {

    @Test
    fun resolveSaveDecision_favoritesAppRecipePosts() {
        val decision = RecipeCommunitySaveResolver.resolveSaveDecision(
            RecipeCommunitySaveResolver.SaveDecisionInput(
                ownerUid = "me",
                postUserId = "other",
                recipeTitle = "Tarif",
                recipeIngredients = emptyList(),
                recipeSteps = emptyList(),
                recipeSourceTag = RecipeRepository.RECIPE_SOURCE_TAG_APP,
                sourceOwnerUid = null,
                sourceType = null
            )
        )

        assertEquals(RecipeCommunitySaveResolver.SaveDecision.FAVORITE_SOURCE, decision)
    }

    @Test
    fun resolveSaveDecision_storesCommunityPayloadFromOtherUsers() {
        val decision = RecipeCommunitySaveResolver.resolveSaveDecision(
            RecipeCommunitySaveResolver.SaveDecisionInput(
                ownerUid = "me",
                postUserId = "other",
                recipeTitle = "  Topluluk Tarifi  ",
                recipeIngredients = listOf(" 1 adet yumurta "),
                recipeSteps = emptyList(),
                recipeSourceTag = null,
                sourceOwnerUid = null,
                sourceType = null
            )
        )

        assertEquals(RecipeCommunitySaveResolver.SaveDecision.STORE_COMMUNITY, decision)
    }

    @Test
    fun resolveSaveDecision_ignoresOwnRecipeCopies() {
        val decision = RecipeCommunitySaveResolver.resolveSaveDecision(
            RecipeCommunitySaveResolver.SaveDecisionInput(
                ownerUid = "me",
                postUserId = "other",
                recipeTitle = "Tarif",
                recipeIngredients = listOf("Pirinç"),
                recipeSteps = emptyList(),
                recipeSourceTag = RecipeRepository.RECIPE_SOURCE_TAG_COMMUNITY,
                sourceOwnerUid = "me",
                sourceType = RecipeEntity.SOURCE_TYPE_USER
            )
        )

        assertEquals(RecipeCommunitySaveResolver.SaveDecision.IGNORE, decision)
    }

    @Test
    fun buildIds_sanitizesFirestoreUnsafeCharacters() {
        assertEquals(
            "shared_owner_post_1_2_3_4_5_10",
            RecipeCommunitySaveResolver.buildSharedRecipeId(
                ownerUid = "owner",
                postId = "post/1.2#3\$4[5]",
                recipeId = "recipe",
                now = 10L
            )
        )
        assertEquals(
            "community_preview_recipe_1_2",
            RecipeCommunitySaveResolver.buildPreviewRecipeId(
                postId = "",
                recipeId = "recipe/1.2"
            )
        )
    }

    @Test
    fun resolveCommunityValues_cleansPostPayloadAndExtractsIngredientNames() {
        val values = RecipeCommunitySaveResolver.resolveCommunityValues(
            recipeTitle = "  Fit Bowl  ",
            recipeDescription = "  ",
            recipeDuration = "  ",
            recipeServing = " 2 porsiyon ",
            recipeCalories = null,
            recipeCategory = " ana yemek ",
            recipeIngredients = listOf(" 1 su bardağı pirinç ", "", "2 adet yumurta"),
            recipeSteps = listOf("  Karıştır  ", ""),
            source = null,
            defaultDescription = "Topluluktan kaydedilen tarif"
        )

        assertEquals("Fit Bowl", values.title)
        assertEquals("Topluluktan kaydedilen tarif", values.description)
        assertEquals("15-30 dk", values.durationLabel)
        assertEquals("2 porsiyon", values.servingDisplay)
        assertEquals("ana yemek", values.category)
        assertEquals(0, values.calories)
        assertEquals(listOf("1 su bardağı pirinç", "2 adet yumurta"), values.ingredients)
        assertEquals(listOf("pirinç", "yumurta"), values.ingredientNames)
        assertEquals(listOf("Karıştır"), values.steps)
    }

    @Test
    fun resolveCommunityValues_preservesSourceIngredientsWhenOverrideIsMissing() {
        val source = recipeEntity(
            ingredientNames = listOf("Kaynak Pirinç"),
            ingredients = listOf("1 kase kaynak pirinç"),
            steps = listOf("Kaynak adım")
        )

        val values = RecipeCommunitySaveResolver.resolveCommunityValues(
            recipeTitle = null,
            recipeDescription = null,
            recipeDuration = null,
            recipeServing = null,
            recipeCalories = null,
            recipeCategory = null,
            recipeIngredients = emptyList(),
            recipeSteps = emptyList(),
            source = source,
            defaultDescription = "Topluluktan kaydedilen tarif"
        )

        assertEquals("Kaynak Tarif", values.title)
        assertEquals(listOf("1 kase kaynak pirinç"), values.ingredients)
        assertEquals(listOf("Kaynak Pirinç"), values.ingredientNames)
        assertEquals(listOf("Kaynak adım"), values.steps)
    }

    private fun recipeEntity(
        ingredientNames: List<String>,
        ingredients: List<String>,
        steps: List<String>
    ): RecipeEntity {
        return RecipeEntity(
            id = "source",
            ownerUid = "other",
            sourceType = RecipeEntity.SOURCE_TYPE_USER,
            sourceRecipeId = null,
            sourceUserId = null,
            sourcePostId = null,
            title = "Kaynak Tarif",
            description = "Açıklama",
            mainCategoryKey = "ana yemek",
            subcategoryKey = "",
            subcategoryKeys = emptySet(),
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = 25,
            durationLabel = "15-30 dk",
            difficultyKey = "kolay",
            calories = 120,
            imageResId = 0,
            servings = 1,
            servingDisplay = "1 porsiyon",
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0,
            ingredientNames = ingredientNames,
            ingredients = ingredients,
            steps = steps,
            tip = null,
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}
