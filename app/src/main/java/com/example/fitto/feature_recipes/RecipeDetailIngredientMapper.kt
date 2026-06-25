package com.example.fitto.feature_recipes

import java.util.Locale
import kotlin.math.roundToInt

object RecipeDetailIngredientMapper {

    fun fromDisplayLine(
        line: String,
        index: Int
    ): RecipeDetailIngredient {
        val raw = line.trim()
        val tokens = raw.split(Regex("\\s+")).filter { token -> token.isNotBlank() }
        val amountParts = mutableListOf<String>()
        var nameStart = 0

        if (tokens.isNotEmpty() && looksLikeAmount(tokens.first())) {
            amountParts.add(tokens.first())
            nameStart = 1

            while (
                nameStart < tokens.size &&
                tokens[nameStart].lowercase(TURKISH_LOCALE) in UNIT_WORDS &&
                amountParts.size < MAX_AMOUNT_PARTS
            ) {
                amountParts.add(tokens[nameStart])
                nameStart++
            }
        }

        val name = tokens.drop(nameStart).joinToString(" ").ifBlank { raw }
        val amount = amountParts.joinToString(" ")
        val displayName = name.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
        }

        return RecipeDetailIngredient(
            id = "ingredient_${index}_${raw.hashCode()}",
            icon = iconForIngredient(displayName),
            name = displayName,
            amountUnit = amount,
            estimatedCalories = estimateCalories(displayName, amount)
        )
    }

    fun toDisplayLine(ingredient: RecipeDetailIngredient): String {
        return listOf(ingredient.amountUnit, ingredient.name)
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() && value != EMPTY_VALUE_MARKER }
            .joinToString(" ")
            .ifBlank { ingredient.name }
    }

    fun splitAmountAndUnit(amountUnit: String): Pair<String, String> {
        val parts = amountUnit.trim()
            .split(Regex("\\s+"))
            .filter { part -> part.isNotBlank() }

        if (parts.isEmpty()) return "" to ""

        return if (looksLikeAmount(parts.first())) {
            parts.first() to parts.drop(1).joinToString(" ")
        } else {
            "" to parts.joinToString(" ")
        }
    }

    fun withUpdatedAmount(
        ingredient: RecipeDetailIngredient,
        amount: String,
        unit: String
    ): RecipeDetailIngredient {
        val newAmountUnit = listOf(amount.trim(), unit.trim())
            .filter { value -> value.isNotBlank() }
            .joinToString(" ")

        return ingredient.copy(
            amountUnit = newAmountUnit,
            estimatedCalories = estimateCalories(
                name = ingredient.name,
                amountUnit = newAmountUnit
            )
        )
    }

    fun estimateCalories(
        name: String,
        amountUnit: String
    ): Int {
        val normalizedName = name.lowercase(TURKISH_LOCALE)
        val normalizedAmount = amountUnit.lowercase(TURKISH_LOCALE)
        val amount = extractAmountValue(normalizedAmount)
        val kcalPer100 = kcalPer100Gram(normalizedName)
        val grams = approximateGrams(normalizedName, normalizedAmount, amount)

        if (grams != null) {
            return ((grams / 100.0) * kcalPer100).roundToInt().coerceAtLeast(0)
        }

        return fallbackIngredientCalories(normalizedName)
    }

    private fun looksLikeAmount(value: String): Boolean {
        return value.any { char -> char.isDigit() } || value.contains("/")
    }

    private fun iconForIngredient(name: String): String {
        val normalized = name.lowercase(TURKISH_LOCALE)
        return when {
            "lahana" in normalized || "marul" in normalized || "ıspanak" in normalized -> "🥬"
            "havuç" in normalized -> "🥕"
            "patates" in normalized -> "🥔"
            "soğan" in normalized -> "🧅"
            "domates" in normalized -> "🍅"
            "biber" in normalized -> "🫑"
            "tavuk" in normalized -> "🍗"
            "et" in normalized || "kıyma" in normalized -> "🥩"
            "balık" in normalized -> "🐟"
            "yumurta" in normalized -> "🥚"
            "süt" in normalized || "yoğurt" in normalized || "peynir" in normalized -> "🥛"
            "zeytinya" in normalized || "yağ" in normalized -> "🫒"
            "muz" in normalized -> "🍌"
            "elma" in normalized -> "🍎"
            "mercimek" in normalized || "nohut" in normalized || "fasulye" in normalized -> "🫘"
            "un" in normalized || "yulaf" in normalized || "pirinç" in normalized ||
                "bulgur" in normalized -> "🌾"
            else -> "🥄"
        }
    }

    private fun extractAmountValue(amountUnit: String): Double? {
        val token = amountUnit
            .split(Regex("\\s+"))
            .firstOrNull { part -> part.any { char -> char.isDigit() } }
            ?: return null
        val normalized = token.replace(',', '.')

        if ("/" in normalized) {
            val parts = normalized.split("/")
            val numerator = parts.getOrNull(0)?.toDoubleOrNull()
            val denominator = parts.getOrNull(1)?.toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) {
                return numerator / denominator
            }
        }

        return normalized.toDoubleOrNull()
    }

    private fun approximateGrams(
        name: String,
        amountUnit: String,
        amount: Double?
    ): Double? {
        if (amount == null) return null

        return when {
            "kg" in amountUnit -> amount * 1000.0
            "g" in amountUnit || "gr" in amountUnit || "gram" in amountUnit -> amount
            "lt" in amountUnit || "litre" in amountUnit -> amount * 1000.0
            "ml" in amountUnit -> amount
            "su barda" in amountUnit -> amount * 200.0
            "çay barda" in amountUnit -> amount * 100.0
            "yemek kaşı" in amountUnit -> amount * 15.0
            "tatlı kaşı" in amountUnit -> amount * 10.0
            "çay kaşı" in amountUnit -> amount * 5.0
            "dilim" in amountUnit -> amount * 30.0
            "demet" in amountUnit -> amount * 50.0
            "diş" in amountUnit -> amount * 3.0
            "adet" in amountUnit -> amount * approximateItemGrams(name)
            else -> null
        }
    }

    private fun approximateItemGrams(name: String): Double {
        return when {
            "yumurta" in name -> 55.0
            "soğan" in name -> 90.0
            "patates" in name -> 150.0
            "havuç" in name -> 70.0
            "domates" in name -> 110.0
            "biber" in name -> 60.0
            "muz" in name -> 120.0
            "elma" in name -> 160.0
            "tavuk but" in name -> 150.0
            "tavuk" in name -> 120.0
            else -> 80.0
        }
    }

    private fun kcalPer100Gram(name: String): Int {
        return when {
            "zeytinya" in name || "sıvı yağ" in name || "tereya" in name ||
                "yağ" in name -> 884
            "şeker" in name || "bal" in name || "pekmez" in name -> 310
            "fıstık" in name || "ceviz" in name || "badem" in name ||
                "fındık" in name -> 600
            "kıyma" in name || "dana" in name || "kuzu" in name || "et" in name -> 250
            "tavuk" in name || "hindi" in name -> 165
            "balık" in name -> 160
            "yumurta" in name -> 143
            "peynir" in name -> 260
            "yoğurt" in name -> 60
            "süt" in name -> 50
            "pirinç" in name || "bulgur" in name || "makarna" in name ||
                "erişte" in name -> 350
            "un" in name || "galeta" in name || "yulaf" in name -> 360
            "nohut" in name || "fasulye" in name || "mercimek" in name -> 165
            "patates" in name -> 77
            "muz" in name -> 89
            "elma" in name -> 52
            "havuç" in name -> 41
            "soğan" in name -> 40
            "domates" in name -> 18
            "biber" in name -> 25
            "lahana" in name || "kabak" in name || "salatalık" in name -> 24
            "tuz" in name || "karabiber" in name || "kimyon" in name ||
                "nane" in name || "pul biber" in name -> 0
            else -> 60
        }
    }

    private fun fallbackIngredientCalories(name: String): Int {
        return when {
            "yağ" in name -> 120
            "fıstık" in name || "ceviz" in name || "badem" in name -> 110
            "tavuk" in name || "et" in name || "kıyma" in name -> 95
            "patates" in name -> 80
            "yumurta" in name -> 70
            "peynir" in name -> 65
            "süt" in name || "yoğurt" in name -> 55
            "pirinç" in name || "bulgur" in name || "makarna" in name ||
                "yulaf" in name -> 90
            "muz" in name -> 90
            "havuç" in name || "soğan" in name || "domates" in name ||
                "biber" in name -> 35
            else -> 40
        }
    }

    private val TURKISH_LOCALE = Locale.forLanguageTag("tr-TR")
    private const val EMPTY_VALUE_MARKER = "—"
    private const val MAX_AMOUNT_PARTS = 5

    private val UNIT_WORDS = setOf(
        "adet",
        "g",
        "gr",
        "gram",
        "kg",
        "ml",
        "lt",
        "litre",
        "su",
        "bardağı",
        "bardak",
        "küçük",
        "yemek",
        "tatlı",
        "çay",
        "kaşığı",
        "kaşık",
        "dilim",
        "demet",
        "tutam",
        "avuç",
        "paket",
        "kase",
        "diş"
    )
}
