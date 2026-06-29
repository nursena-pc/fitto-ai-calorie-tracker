package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeCommunityShareResolverTest {

    @Test
    fun `cleanShareIngredients uses non blank override when provided`() {
        val result = RecipeCommunityShareResolver.cleanShareIngredients(
            ingredientsOverride = listOf("  1 adet yumurta ", " ", "100 g lor "),
            sourceIngredients = listOf("kaynak")
        )

        assertEquals(listOf("1 adet yumurta", "100 g lor"), result)
    }

    @Test
    fun `cleanShareIngredients keeps source ingredients when override is null`() {
        val sourceIngredients = listOf("2 dilim ekmek", "domates")

        val result = RecipeCommunityShareResolver.cleanShareIngredients(
            ingredientsOverride = null,
            sourceIngredients = sourceIngredients
        )

        assertEquals(sourceIngredients, result)
    }

    @Test
    fun `buildShareContent includes trimmed comment title and ingredient preview`() {
        val result = RecipeCommunityShareResolver.buildShareContent(
            RecipeCommunityShareResolver.ShareContentInput(
                comment = "  Çok pratik oldu  ",
                recipeTitle = "Fit Omlet",
                ingredients = listOf("2 adet yumurta", "100 g lor peyniri")
            )
        )

        assertEquals(
            "Çok pratik oldu\n\n" +
                "Denediğim tarifi toplulukla paylaşıyorum: Fit Omlet\n\n" +
                "Malzemeler: yumurta, lor peyniri",
            result
        )
    }

    @Test
    fun `buildShareContent omits blank comment and ingredients section`() {
        val result = RecipeCommunityShareResolver.buildShareContent(
            RecipeCommunityShareResolver.ShareContentInput(
                comment = " ",
                recipeTitle = "Salata",
                ingredients = emptyList()
            )
        )

        assertEquals("Denediğim tarifi toplulukla paylaşıyorum: Salata", result)
    }

    @Test
    fun `buildShareContent limits ingredient preview to eight items`() {
        val result = RecipeCommunityShareResolver.buildShareContent(
            RecipeCommunityShareResolver.ShareContentInput(
                comment = "",
                recipeTitle = "Bol Malzemeli Tarif",
                ingredients = (1..9).map { index -> "$index adet malzeme$index" }
            )
        )

        assertEquals(
            "Denediğim tarifi toplulukla paylaşıyorum: Bol Malzemeli Tarif\n\n" +
                "Malzemeler: malzeme1, malzeme2, malzeme3, malzeme4, malzeme5, malzeme6, malzeme7, malzeme8",
            result
        )
    }

    @Test
    fun `sourceTagForRecipe marks seed recipes as app recipes`() {
        assertEquals(
            "app_recipe",
            RecipeCommunityShareResolver.sourceTagForRecipe(RecipeEntity.SOURCE_TYPE_SEED)
        )
    }

    @Test
    fun `sourceTagForRecipe marks non seed recipes as community recipes`() {
        assertEquals(
            "community_recipe",
            RecipeCommunityShareResolver.sourceTagForRecipe(RecipeEntity.SOURCE_TYPE_USER)
        )
    }

    @Test
    fun `servingText falls back to servings when display is blank`() {
        assertEquals("3 kişilik", RecipeCommunityShareResolver.servingText(" ", 3))
    }

    @Test
    fun `tagsForRecipe keeps recipe tag and main category`() {
        assertEquals(listOf("Tarif", "kahvalti"), RecipeCommunityShareResolver.tagsForRecipe("kahvalti"))
    }
}
