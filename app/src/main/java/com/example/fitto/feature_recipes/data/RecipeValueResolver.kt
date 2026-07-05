package com.example.fitto.feature_recipes.data

import com.example.fitto.core.text.AppLocales
import java.util.Locale
import kotlin.math.roundToInt

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
        val normalizedLabel = durationLabel.trim().lowercase(TURKISH_LOCALE)
        return FIXED_DURATION_MINUTES[normalizedLabel]
            ?: representativeRangeMinutes(normalizedLabel)
            ?: DEFAULT_DURATION_MINUTES
    }

    private fun representativeRangeMinutes(durationLabel: String): Int? {
        val match = DURATION_RANGE_REGEX.matchEntire(durationLabel) ?: return null
        val startMinutes = match.groupValues[1].toIntOrNull() ?: return null
        val endMinutes = match.groupValues[2].toIntOrNull() ?: return null
        if (startMinutes > endMinutes) return null

        val midpoint = (startMinutes.toLong() + endMinutes.toLong()) / 2.0
        return (midpoint / DURATION_ROUNDING_STEP_MINUTES).roundToInt() *
            DURATION_ROUNDING_STEP_MINUTES
    }

    private const val MAX_AMOUNT_TOKEN_SCAN_COUNT = 5
    private const val DEFAULT_DURATION_MINUTES = 30
    private const val DURATION_ROUNDING_STEP_MINUTES = 5

    private val TURKISH_LOCALE = AppLocales.TURKISH

    private val DURATION_RANGE_REGEX = Regex("""^(\d+)\s*[-–—]\s*(\d+)\s*dk$""")

    private val FIXED_DURATION_MINUTES = mapOf(
        "15 dk altı" to 10,
        "60 dk üzeri" to 75
    )

    private val INGREDIENT_AMOUNT_WORDS = setOf(
        "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
        "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
        "avuç", "paket", "kase", "diş"
    )
}
