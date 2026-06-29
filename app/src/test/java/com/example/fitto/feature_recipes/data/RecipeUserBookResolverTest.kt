package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeUserBookResolverTest {

    @Test
    fun buildUserRecipeEntity_cleansDraftAndAppliesDefaults() {
        val entity = RecipeUserBookResolver.buildUserRecipeEntity(
            RecipeUserBookResolver.UserRecipeEntityInput(
                draft = UserRecipeDraft(
                    title = "   ",
                    description = "  ",
                    mainCategoryKey = "kahvalti",
                    durationLabel = "15-30 dk",
                    difficultyKey = "kolay",
                    ingredients = listOf("  2 adet yumurta - protein ", " ", " Domates "),
                    steps = listOf("  Karıştır ", "", "Pişir  ")
                ),
                ownerUid = "user/1.2#3\$4[5]",
                now = 100L,
                defaultImageResId = 42
            )
        )

        assertEquals("user_user_1_2_3_4_5__100", entity.id)
        assertEquals("Yeni Tarif", entity.title)
        assertEquals("Kullanıcı tarafından eklenen tarif", entity.description)
        assertEquals(listOf("2 adet yumurta - protein", "Domates"), entity.ingredients)
        assertEquals(listOf("2 adet yumurta", "Domates"), entity.ingredientNames)
        assertEquals(listOf("Karıştır", "Pişir"), entity.steps)
        assertEquals(25, entity.durationMinutes)
        assertEquals(42, entity.imageResId)
        assertEquals(RecipeEntity.SOURCE_TYPE_USER, entity.sourceType)
    }

    @Test
    fun buildCopiedRecipeEntity_keepsSourceIngredientsWhenOverrideIsMissing() {
        val source = recipe(
            id = "seed_1",
            sourceRecipeId = "1",
            title = "Fit Omlet",
            ingredientNames = listOf("Yumurta", "Lor"),
            ingredients = listOf("2 adet yumurta", "100 g lor")
        )

        val entity = RecipeUserBookResolver.buildCopiedRecipeEntity(
            RecipeUserBookResolver.CopyRecipeEntityInput(
                source = source,
                ownerUid = "me",
                now = 200L,
                ingredientsOverride = null,
                resolvedImageResId = 77
            )
        )

        assertEquals("copied_me_seed_1_200", entity.id)
        assertEquals("Fit Omlet", entity.title)
        assertEquals("1", entity.sourceRecipeId)
        assertEquals(listOf("2 adet yumurta", "100 g lor"), entity.ingredients)
        assertEquals(listOf("Yumurta", "Lor"), entity.ingredientNames)
        assertEquals(77, entity.imageResId)
        assertEquals(RecipeEntity.SOURCE_TYPE_COPIED, entity.sourceType)
    }

    @Test
    fun buildCopiedRecipeEntity_cleansOverrideAndDerivesIngredientNames() {
        val source = recipe(
            id = "custom/1",
            sourceRecipeId = null,
            title = "Salata",
            ingredientNames = listOf("Kaynak"),
            ingredients = listOf("Kaynak")
        )

        val entity = RecipeUserBookResolver.buildCopiedRecipeEntity(
            RecipeUserBookResolver.CopyRecipeEntityInput(
                source = source,
                ownerUid = "me.1",
                now = 300L,
                ingredientsOverride = listOf(" 1 adet domates ", " 100 g lor peyniri ", " ", "1/2 avokado"),
                resolvedImageResId = 88
            )
        )

        assertEquals("copied_me_1_custom_1_300", entity.id)
        assertEquals("Salata (Bana Özel)", entity.title)
        assertEquals("custom/1", entity.sourceRecipeId)
        assertEquals(listOf("1 adet domates", "100 g lor peyniri", "1/2 avokado"), entity.ingredients)
        assertEquals(listOf("domates", "lor peyniri", "avokado"), entity.ingredientNames)
        assertEquals(88, entity.imageResId)
    }

    private fun recipe(
        id: String,
        sourceRecipeId: String?,
        title: String,
        ingredientNames: List<String>,
        ingredients: List<String>
    ): RecipeEntity {
        return RecipeEntity(
            id = id,
            ownerUid = null,
            sourceType = RecipeEntity.SOURCE_TYPE_SEED,
            sourceRecipeId = sourceRecipeId,
            sourceUserId = null,
            sourcePostId = null,
            title = title,
            description = "desc",
            mainCategoryKey = "kahvalti",
            subcategoryKey = "",
            subcategoryKeys = emptySet(),
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = 25,
            durationLabel = "15-30 dk",
            difficultyKey = "kolay",
            calories = 120,
            imageResId = 11,
            servings = 1,
            servingDisplay = "1 porsiyon",
            proteinGrams = 10,
            carbGrams = 5,
            fatGrams = 7,
            ingredientNames = ingredientNames,
            ingredients = ingredients,
            steps = listOf("step"),
            tip = null,
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}
