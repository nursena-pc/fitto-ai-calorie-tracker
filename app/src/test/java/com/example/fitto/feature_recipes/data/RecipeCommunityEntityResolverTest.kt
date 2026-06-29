package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeCommunityEntityResolverTest {

    @Test
    fun buildCommunityRecipeEntity_copiesSourceRecipeWithCommunityOwnershipAndResolvedImage() {
        val source = recipeEntity(
            id = "source-1",
            ownerUid = "original-owner",
            sourceRecipeId = "root-recipe",
            sourceUserId = "root-user",
            sourcePostId = "root-post",
            imageResId = 10,
            createdAt = 1L,
            updatedAt = 2L
        )
        val values = communityValues(
            title = "Topluluk Bowl",
            description = "Yeni açıklama",
            calories = 450,
            ingredients = listOf("1 kase nohut"),
            ingredientNames = listOf("nohut"),
            steps = listOf("Karıştır")
        )

        val entity = RecipeCommunityEntityResolver.buildCommunityRecipeEntity(
            RecipeCommunityEntityResolver.CommunityEntityInput(
                id = "shared-id",
                ownerUid = "me",
                source = source,
                values = values,
                postUserId = "post-owner",
                postId = "post-1",
                recipeId = "source-1",
                recipeImageResId = null,
                fallbackImageResId = 99,
                resolvedSourceImageResId = 42,
                now = 100L
            )
        )

        assertEquals("shared-id", entity.id)
        assertEquals("me", entity.ownerUid)
        assertEquals(RecipeEntity.SOURCE_TYPE_COMMUNITY, entity.sourceType)
        assertEquals("root-recipe", entity.sourceRecipeId)
        assertEquals("post-owner", entity.sourceUserId)
        assertEquals("post-1", entity.sourcePostId)
        assertEquals("Topluluk Bowl", entity.title)
        assertEquals("Yeni açıklama", entity.description)
        assertEquals(450, entity.calories)
        assertEquals(42, entity.imageResId)
        assertEquals(listOf("1 kase nohut"), entity.ingredients)
        assertEquals(listOf("nohut"), entity.ingredientNames)
        assertEquals(listOf("Karıştır"), entity.steps)
        assertEquals("ana yemek", entity.mainCategoryKey)
        assertEquals(100L, entity.createdAt)
        assertEquals(100L, entity.updatedAt)
    }

    @Test
    fun buildCommunityRecipeEntity_buildsPostOnlyCommunityRecipeWithFallbackValues() {
        val values = communityValues(
            durationLabel = "30-45 dk",
            servingDisplay = "2 porsiyon",
            category = "kahvalti",
            ingredients = listOf("2 adet yumurta"),
            ingredientNames = listOf("yumurta"),
            steps = listOf("Pişir")
        )

        val entity = RecipeCommunityEntityResolver.buildCommunityRecipeEntity(
            RecipeCommunityEntityResolver.CommunityEntityInput(
                id = "preview-id",
                ownerUid = null,
                source = null,
                values = values,
                postUserId = "user-1",
                postId = " post-1 ",
                recipeId = " recipe-1 ",
                recipeImageResId = null,
                fallbackImageResId = 77,
                resolvedSourceImageResId = null,
                now = 200L
            )
        )

        assertEquals("preview-id", entity.id)
        assertNull(entity.ownerUid)
        assertEquals(RecipeEntity.SOURCE_TYPE_COMMUNITY, entity.sourceType)
        assertEquals("recipe-1", entity.sourceRecipeId)
        assertEquals("user-1", entity.sourceUserId)
        assertEquals("post-1", entity.sourcePostId)
        assertEquals("kahvalti", entity.mainCategoryKey)
        assertEquals(40, entity.durationMinutes)
        assertEquals("kolay", entity.difficultyKey)
        assertEquals(77, entity.imageResId)
        assertEquals(1, entity.servings)
        assertEquals("2 porsiyon", entity.servingDisplay)
        assertEquals(0, entity.proteinGrams)
        assertEquals(0, entity.carbGrams)
        assertEquals(0, entity.fatGrams)
        assertNull(entity.tip)
        assertEquals(200L, entity.createdAt)
        assertEquals(200L, entity.updatedAt)
    }

    @Test
    fun buildCommunityRecipeEntity_prefersPayloadImageForPostOnlyRecipes() {
        val entity = RecipeCommunityEntityResolver.buildCommunityRecipeEntity(
            RecipeCommunityEntityResolver.CommunityEntityInput(
                id = "shared-id",
                ownerUid = "me",
                source = null,
                values = communityValues(),
                postUserId = "",
                postId = "",
                recipeId = "",
                recipeImageResId = 12,
                fallbackImageResId = 77,
                resolvedSourceImageResId = null,
                now = 1L
            )
        )

        assertEquals(12, entity.imageResId)
        assertNull(entity.sourceRecipeId)
        assertNull(entity.sourceUserId)
        assertNull(entity.sourcePostId)
    }

    private fun communityValues(
        title: String = "Tarif",
        description: String = "Açıklama",
        durationLabel: String = "15-30 dk",
        servingDisplay: String = "1 porsiyon",
        category: String = "ana yemek",
        calories: Int = 120,
        ingredients: List<String> = listOf("1 kase malzeme"),
        ingredientNames: List<String> = listOf("malzeme"),
        steps: List<String> = listOf("Hazırla")
    ): RecipeCommunitySaveResolver.CommunityRecipeValues {
        return RecipeCommunitySaveResolver.CommunityRecipeValues(
            title = title,
            description = description,
            durationLabel = durationLabel,
            servingDisplay = servingDisplay,
            category = category,
            calories = calories,
            ingredients = ingredients,
            ingredientNames = ingredientNames,
            steps = steps
        )
    }

    private fun recipeEntity(
        id: String = "source",
        ownerUid: String? = "owner",
        sourceRecipeId: String? = null,
        sourceUserId: String? = null,
        sourcePostId: String? = null,
        imageResId: Int = 0,
        createdAt: Long = 1L,
        updatedAt: Long = 1L
    ): RecipeEntity {
        return RecipeEntity(
            id = id,
            ownerUid = ownerUid,
            sourceType = RecipeEntity.SOURCE_TYPE_USER,
            sourceRecipeId = sourceRecipeId,
            sourceUserId = sourceUserId,
            sourcePostId = sourcePostId,
            title = "Kaynak Tarif",
            description = "Açıklama",
            mainCategoryKey = "ana yemek",
            subcategoryKey = "sebze",
            subcategoryKeys = setOf("sebze"),
            mainIngredientKeys = setOf("nohut"),
            dietKeys = setOf("vegan"),
            methodKeys = setOf("firin"),
            durationMinutes = 25,
            durationLabel = "15-30 dk",
            difficultyKey = "orta",
            calories = 120,
            imageResId = imageResId,
            servings = 2,
            servingDisplay = "2 porsiyon",
            proteinGrams = 10,
            carbGrams = 20,
            fatGrams = 5,
            ingredientNames = listOf("nohut"),
            ingredients = listOf("1 kase nohut"),
            steps = listOf("Pişir"),
            tip = "İpucu",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
