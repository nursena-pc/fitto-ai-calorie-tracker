package com.example.fitto.feature_recipes.data

import com.example.fitto.data.local.entity.RecipeEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeSeedResolverTest {

    @Test
    fun buildSeedRecipeEntity_mapsSeedFieldsAndFallbacks() {
        val json = JSONObject(
            """
            {
              "id": 12,
              "tarif_adi": "  Mercimek Çorbası  ",
              "labels": {
                "ana_kategori": "çorba",
                "alt_kategori_etiketleri": ["  sıcak  ", ""],
                "ana_malzeme_etiketleri": ["mercimek"],
                "diyet_etiketleri": ["vegan"],
                "kontrol_notlari": ["Kısık ateşte pişir", ""]
              },
              "hazirlama_yontemi": ["tencere", ""],
              "sure_etiketi": "30-60 dk",
              "porsiyon_gosterim": " 4 kişilik ",
              "porsiyon_min": 4,
              "zorluk": "orta",
              "malzemeler": [
                {"isim": " kırmızı mercimek ", "miktar": 1.0, "birim": "su bardağı"},
                {"isim": "", "miktar": 2, "birim": "adet"},
                {"isim": "tuz"}
              ],
              "yapilis_adimlari": [" Yıka ", "", " Pişir "]
            }
            """.trimIndent()
        )

        val entity = RecipeSeedResolver.buildSeedRecipeEntity(
            RecipeSeedResolver.SeedRecipeInput(
                jsonObject = json,
                index = 0,
                now = 123L,
                imageResId = 456
            )
        )

        assertEquals("seed_12", entity.id)
        assertNull(entity.ownerUid)
        assertEquals(RecipeEntity.SOURCE_TYPE_SEED, entity.sourceType)
        assertEquals("12", entity.sourceRecipeId)
        assertEquals("Mercimek Çorbası", entity.title)
        assertEquals("çorba", entity.mainCategoryKey)
        assertEquals("sıcak", entity.subcategoryKey)
        assertEquals(setOf("sıcak"), entity.subcategoryKeys)
        assertEquals(setOf("mercimek"), entity.mainIngredientKeys)
        assertEquals(setOf("vegan"), entity.dietKeys)
        assertEquals(setOf("tencere"), entity.methodKeys)
        assertEquals("Sıcak • 30-60 dk • 4 kişilik", entity.description)
        assertEquals(45, entity.durationMinutes)
        assertEquals("30-60 dk", entity.durationLabel)
        assertEquals("orta", entity.difficultyKey)
        assertEquals(456, entity.imageResId)
        assertEquals(4, entity.servings)
        assertEquals("4 kişilik", entity.servingDisplay)
        assertEquals(listOf("kırmızı mercimek", "tuz"), entity.ingredientNames)
        assertEquals(listOf("1 su bardağı kırmızı mercimek", "tuz"), entity.ingredients)
        assertEquals(listOf("Yıka", "Pişir"), entity.steps)
        assertEquals("Kısık ateşte pişir", entity.tip)
        assertEquals(123L, entity.createdAt)
        assertEquals(123L, entity.updatedAt)
    }

    @Test
    fun buildSeedRecipeEntity_usesDefaultsForMissingOptionalFields() {
        val json = JSONObject("{}")

        val entity = RecipeSeedResolver.buildSeedRecipeEntity(
            RecipeSeedResolver.SeedRecipeInput(
                jsonObject = json,
                index = 2,
                now = 9L,
                imageResId = 77
            )
        )

        assertEquals("seed_3", entity.id)
        assertEquals("Tarif 3", entity.title)
        assertEquals("", entity.mainCategoryKey)
        assertEquals(" • 15-30 dk • 1 kişilik", entity.description)
        assertEquals(25, entity.durationMinutes)
        assertEquals("15-30 dk", entity.durationLabel)
        assertEquals("kolay", entity.difficultyKey)
        assertEquals(1, entity.servings)
        assertEquals("1 kişilik", entity.servingDisplay)
        assertEquals(emptyList<String>(), entity.ingredientNames)
        assertEquals(emptyList<String>(), entity.ingredients)
        assertEquals(emptyList<String>(), entity.steps)
        assertNull(entity.tip)
    }

}
