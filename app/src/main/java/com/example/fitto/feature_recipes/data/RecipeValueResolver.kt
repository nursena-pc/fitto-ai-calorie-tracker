package com.example.fitto.feature_recipes.data

import java.util.Locale

object RecipeValueResolver {

    fun safeRecipeId(value: String): String {
        return value
            .replace("/", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    fun cleanLines(lines: List<String>): List<String> {
        return lines.map { line -> line.trim() }
            .filter { line -> line.isNotBlank() }
    }

    fun ingredientNameForCopy(ingredientLine: String): String {
        val cleanLine = ingredientLine.trim()
        val tokens = cleanLine.split(Regex("\\s+")).filter { token -> token.isNotBlank() }
        if (tokens.isEmpty()) return cleanLine

        var nameStart = 0
        if (tokens.first().any { char -> char.isDigit() } || "/" in tokens.first()) {
            nameStart = 1
            while (
                nameStart < tokens.size &&
                tokens[nameStart].lowercase(TURKISH_LOCALE) in INGREDIENT_AMOUNT_WORDS &&
                nameStart < MAX_AMOUNT_TOKEN_SCAN_COUNT
            ) {
                nameStart++
            }
        }

        return tokens.drop(nameStart).joinToString(" ").ifBlank { cleanLine }
    }

    fun representativeMinutes(durationLabel: String): Int {
        return when (durationLabel) {
            "15 dk altı" -> 10
            "15-30 dk" -> 25
            "30-60 dk" -> 45
            "60 dk üzeri" -> 75
            else -> 30
        }
    }

    private const val MAX_AMOUNT_TOKEN_SCAN_COUNT = 5

    private val TURKISH_LOCALE = Locale("tr", "TR")

    private val INGREDIENT_AMOUNT_WORDS = setOf(
        "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
        "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
        "avuç", "paket", "kase", "diş"
    )
}
