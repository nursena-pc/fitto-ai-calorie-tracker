package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipePresentationResolverTest {

    @Test
    fun toUiModel_mapsEntityFieldsAndFavoriteState() {
        val entity = recipeEntity(
            id = "seed_7",
            title = "Omlet",
            description = "Proteinli kahvaltı",
            imageResId = 12,
            sourceType = RecipeEntity.SOURCE_TYPE_SEED,
            sourceRecipeId = "7"
        )

        val model = RecipePresentationResolver.toUiModel(
            entity = entity,
            isFavorite = true,
            imageResId = 99
        )

        assertEquals("seed_7", model.id)
        assertEquals("Omlet", model.title)
        assertEquals("Proteinli kahvaltı", model.description)
        assertEquals("kahvalti", model.mainCategoryKey)
        assertEquals(setOf("protein"), model.subcategoryKeys)
        assertEquals(setOf("yumurta"), model.mainIngredientKeys)
        assertEquals(setOf("low_carb"), model.dietKeys)
        assertEquals(setOf("tava"), model.methodKeys)
        assertEquals(25, model.durationMinutes)
        assertEquals("15-30 dk", model.durationLabel)
        assertEquals("kolay", model.difficultyKey)
        assertEquals(320, model.calories)
        assertEquals(99, model.imageResId)
        assertEquals(2, model.servings)
        assertEquals("2 kişilik", model.servingDisplay)
        assertEquals(18, model.proteinGrams)
        assertEquals(5, model.carbGrams)
        assertEquals(12, model.fatGrams)
        assertEquals(listOf("yumurta"), model.ingredientNames)
        assertEquals(listOf("2 adet yumurta"), model.ingredients)
        assertEquals(listOf("Çırp", "Pişir"), model.steps)
        assertEquals("Sıcak servis et", model.tip)
        assertEquals(true, model.isFavorite)
    }

    @Test
    fun favoriteOriginalRecipes_keepsOnlyFavoriteSeedRecipes() {
        val seedFavorite = recipeEntity(id = "seed_1", sourceType = RecipeEntity.SOURCE_TYPE_SEED)
        val seedNotFavorite = recipeEntity(id = "seed_2", sourceType = RecipeEntity.SOURCE_TYPE_SEED)
        val userFavorite = recipeEntity(id = "user_1", sourceType = RecipeEntity.SOURCE_TYPE_USER)

        val result = RecipePresentationResolver.favoriteOriginalRecipes(
            entities = listOf(seedFavorite, seedNotFavorite, userFavorite),
            favoriteIds = setOf("seed_1", "user_1")
        )

        assertEquals(listOf(seedFavorite), result)
    }

    @Test
    fun toUiModels_marksFavoritesAndUsesImageResolver() {
        val first = recipeEntity(id = "seed_1")
        val second = recipeEntity(id = "seed_2")

        val models = RecipePresentationResolver.toUiModels(
            entities = listOf(first, second),
            favoriteIds = setOf("seed_2"),
            imageResIdResolver = { entity -> entity.id.removePrefix("seed_").toInt() }
        )

        assertEquals(false, models[0].isFavorite)
        assertEquals(1, models[0].imageResId)
        assertEquals(true, models[1].isFavorite)
        assertEquals(2, models[1].imageResId)
    }

    @Test
    fun rawSeedImageId_preservesExistingPriorityRules() {
        assertEquals(
            "12",
            RecipePresentationResolver.rawSeedImageId(
                recipeEntity(
                    id = "seed_99",
                    sourceType = RecipeEntity.SOURCE_TYPE_SEED,
                    sourceRecipeId = "12"
                )
            )
        )
        assertEquals(
            "99",
            RecipePresentationResolver.rawSeedImageId(
                recipeEntity(
                    id = "seed_99",
                    sourceType = RecipeEntity.SOURCE_TYPE_SEED,
                    sourceRecipeId = null
                )
            )
        )
        assertEquals(
            "7",
            RecipePresentationResolver.rawSeedImageId(
                recipeEntity(
                    id = "community_1",
                    sourceType = RecipeEntity.SOURCE_TYPE_COMMUNITY,
                    sourceRecipeId = "7"
                )
            )
        )
        assertNull(
            RecipePresentationResolver.rawSeedImageId(
                recipeEntity(
                    id = "user_1",
                    sourceType = RecipeEntity.SOURCE_TYPE_USER,
                    sourceRecipeId = "abc"
                )
            )
        )
    }

    @Test
    fun imageResolution_usesResourceNameAndFallbacks() {
        assertEquals("recipe_007", RecipePresentationResolver.recipeImageResourceName("7"))
        assertEquals("recipe_012", RecipePresentationResolver.recipeImageResourceName("012"))
        assertNull(RecipePresentationResolver.recipeImageResourceName("abc"))

        val seed = recipeEntity(
            id = "seed_7",
            sourceType = RecipeEntity.SOURCE_TYPE_SEED,
            sourceRecipeId = "7",
            imageResId = 999
        )
        val userWithImage = recipeEntity(
            id = "user_1",
            sourceType = RecipeEntity.SOURCE_TYPE_USER,
            sourceRecipeId = null,
            imageResId = 55
        )
        val userWithoutImage = userWithImage.copy(imageResId = 0)

        assertEquals(
            70,
            RecipePresentationResolver.resolvedImageResId(
                entity = seed,
                resourceIdForRawId = { rawId -> if (rawId == "7") 70 else null },
                defaultImageResId = 1
            )
        )
        assertEquals(
            1,
            RecipePresentationResolver.resolvedImageResId(
                entity = seed,
                resourceIdForRawId = { null },
                defaultImageResId = 1
            )
        )
        assertEquals(
            55,
            RecipePresentationResolver.resolvedImageResId(
                entity = userWithImage,
                resourceIdForRawId = { null },
                defaultImageResId = 1
            )
        )
        assertEquals(
            1,
            RecipePresentationResolver.resolvedImageResId(
                entity = userWithoutImage,
                resourceIdForRawId = { null },
                defaultImageResId = 1
            )
        )
    }

    @Test
    fun rawImageIdForRecipeId_trimsAndSupportsSeedOrNumericIds() {
        assertEquals("42", RecipePresentationResolver.rawImageIdForRecipeId(" seed_42 "))
        assertEquals("8", RecipePresentationResolver.rawImageIdForRecipeId(" 8 "))
        assertNull(RecipePresentationResolver.rawImageIdForRecipeId("community_8"))
    }

    private fun recipeEntity(
        id: String = "seed_1",
        sourceType: String = RecipeEntity.SOURCE_TYPE_SEED,
        sourceRecipeId: String? = "1",
        title: String = "Tarif",
        description: String = "Açıklama",
        imageResId: Int = 10
    ): RecipeEntity {
        return RecipeEntity(
            id = id,
            ownerUid = null,
            sourceType = sourceType,
            sourceRecipeId = sourceRecipeId,
            sourceUserId = null,
            sourcePostId = null,
            title = title,
            description = description,
            mainCategoryKey = "kahvalti",
            subcategoryKey = "protein",
            subcategoryKeys = setOf("protein"),
            mainIngredientKeys = setOf("yumurta"),
            dietKeys = setOf("low_carb"),
            methodKeys = setOf("tava"),
            durationMinutes = 25,
            durationLabel = "15-30 dk",
            difficultyKey = "kolay",
            calories = 320,
            imageResId = imageResId,
            servings = 2,
            servingDisplay = "2 kişilik",
            proteinGrams = 18,
            carbGrams = 5,
            fatGrams = 12,
            ingredientNames = listOf("yumurta"),
            ingredients = listOf("2 adet yumurta"),
            steps = listOf("Çırp", "Pişir"),
            tip = "Sıcak servis et",
            createdAt = 1L,
            updatedAt = 2L
        )
    }
}
