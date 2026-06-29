package com.example.fitto.feature_recipes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeValueResolverTest {

    @Test
    fun safeRecipeId_replacesFirestoreUnsafeCharacters() {
        assertEquals(
            "user_1_2_3_4_5_6",
            RecipeValueResolver.safeRecipeId("user/1.2#3$4[5]6")
        )
    }

    @Test
    fun cleanLines_trimsAndDropsBlankLines() {
        assertEquals(
            listOf("yumurta", "lor"),
            RecipeValueResolver.cleanLines(listOf(" yumurta ", "", "  ", "lor"))
        )
    }

    @Test
    fun ingredientNameForCopy_removesLeadingAmountWords() {
        assertEquals("domates", RecipeValueResolver.ingredientNameForCopy("1 adet domates"))
        assertEquals("lor peyniri", RecipeValueResolver.ingredientNameForCopy("100 g lor peyniri"))
        assertEquals("avokado", RecipeValueResolver.ingredientNameForCopy("1/2 avokado"))
        assertEquals("Tuz", RecipeValueResolver.ingredientNameForCopy("Tuz"))
    }

    @Test
    fun representativeMinutes_mapsKnownLabelsAndFallsBack() {
        assertEquals(10, RecipeValueResolver.representativeMinutes("15 dk altı"))
        assertEquals(25, RecipeValueResolver.representativeMinutes("15-30 dk"))
        assertEquals(45, RecipeValueResolver.representativeMinutes("30-60 dk"))
        assertEquals(75, RecipeValueResolver.representativeMinutes("60 dk üzeri"))
        assertEquals(30, RecipeValueResolver.representativeMinutes("bilinmeyen"))
    }
}
