package com.example.fitto.feature_recipes.data

import com.example.fitto.core.text.AppLocales
import com.example.fitto.data.local.entity.RecipeEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object RecipeSeedResolver {

    data class SeedRecipeInput(
        val jsonObject: JSONObject,
        val index: Int,
        val now: Long,
        val imageResId: Int
    )

    fun buildSeedRecipeEntity(input: SeedRecipeInput): RecipeEntity {
        val json = input.jsonObject
        val labels: JSONObject = json.optJSONObject("labels") ?: JSONObject()
        val rawId: String = json.optInt("id", input.index + 1).toString()
        val recipeId = "seed_$rawId"
        val title: String = json.optString("tarif_adi", "Tarif $rawId").trim()
        val mainCategory: String = labels.optString("ana_kategori", json.optString("kategori", "")).trim()
        val subcategoryKeys: Set<String> = labels.optStringArray("alt_kategori_etiketleri").toSet()
        val mainIngredientKeys: Set<String> = labels.optStringArray("ana_malzeme_etiketleri").toSet()
        val dietKeys: Set<String> = labels.optStringArray("diyet_etiketleri").toSet()
        val methodKeys: Set<String> = json.optStringArray("hazirlama_yontemi").toSet()
        val durationLabel: String = json.optString("sure_etiketi", "").ifBlank { "15-30 dk" }
        val servingDisplay: String = json.optString("porsiyon_gosterim", "").trim().ifBlank {
            val portion: Int = json.optInt("porsiyon", 1)
            "$portion kişilik"
        }

        val servings: Int = json.optInt("porsiyon_min", json.optInt("porsiyon", 1)).coerceAtLeast(1)
        val ingredients: List<RecipeIngredient> = json.optIngredientArray("malzemeler")
        val ingredientNames: List<String> = ingredients.map { ingredient -> ingredient.name }
        val ingredientLines: List<String> = ingredients.map { ingredient -> ingredient.toDisplayText() }
        val steps: List<String> = json.optStringArray("yapilis_adimlari")
        val description: String = buildDescription(
            mainCategory = mainCategory,
            durationLabel = durationLabel,
            servingDisplay = servingDisplay,
            subcategories = subcategoryKeys
        )

        return RecipeEntity(
            id = recipeId,
            ownerUid = null,
            sourceType = RecipeEntity.SOURCE_TYPE_SEED,
            sourceRecipeId = rawId,
            sourceUserId = null,
            sourcePostId = null,
            title = title,
            description = description,
            mainCategoryKey = mainCategory,
            subcategoryKey = subcategoryKeys.firstOrNull().orEmpty(),
            subcategoryKeys = subcategoryKeys,
            mainIngredientKeys = mainIngredientKeys,
            dietKeys = dietKeys,
            methodKeys = methodKeys,
            durationMinutes = RecipeValueResolver.representativeMinutes(durationLabel),
            durationLabel = durationLabel,
            difficultyKey = json.optString("zorluk", "kolay").ifBlank { "kolay" },
            calories = 0,
            imageResId = input.imageResId,
            servings = servings,
            servingDisplay = servingDisplay,
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0,
            ingredientNames = ingredientNames,
            ingredients = ingredientLines,
            steps = steps,
            tip = labels.optStringArray("kontrol_notlari").firstOrNull(),
            createdAt = input.now,
            updatedAt = input.now
        )
    }

    private fun buildDescription(
        mainCategory: String,
        durationLabel: String,
        servingDisplay: String,
        subcategories: Set<String>
    ): String {
        val readableCategory: String = mainCategory.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
        }
        val firstSubcategory: String? = subcategories.firstOrNull()
        val prefix: String = firstSubcategory?.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
        } ?: readableCategory
        return "$prefix • $durationLabel • $servingDisplay"
    }

    private fun JSONObject.optStringArray(key: String): List<String> {
        val array: JSONArray = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index).trim() }
            .filter { value -> value.isNotBlank() }
    }

    private fun JSONObject.optIngredientArray(key: String): List<RecipeIngredient> {
        val array: JSONArray = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index ->
            val ingredientObject: JSONObject = array.optJSONObject(index) ?: JSONObject()
            RecipeIngredient(
                name = ingredientObject.optString("isim", "").trim(),
                amount = ingredientObject.readAmountText(),
                unit = ingredientObject.optNullableString("birim")
            )
        }.filter { ingredient -> ingredient.name.isNotBlank() }
    }

    private fun JSONObject.readAmountText(): String? {
        if (!has("miktar") || isNull("miktar")) return null
        val rawValue: Any = opt("miktar") ?: return null
        return when (rawValue) {
            is Int -> rawValue.toString()
            is Long -> rawValue.toString()
            is Double -> rawValue.toReadableNumber()
            is Number -> rawValue.toDouble().toReadableNumber()
            else -> rawValue.toString().takeIf { value -> value.isNotBlank() }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { value -> value.isNotBlank() }
    }

    private fun Double.toReadableNumber(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else toString().trimEnd('0').trimEnd('.')
    }

    private data class RecipeIngredient(
        val name: String,
        val amount: String?,
        val unit: String?
    ) {
        fun toDisplayText(): String {
            val amountPart: String = listOfNotNull(amount, unit).joinToString(" ").trim()
            return if (amountPart.isBlank()) name else "$amountPart $name"
        }
    }

    private val TURKISH_LOCALE = AppLocales.TURKISH
}
