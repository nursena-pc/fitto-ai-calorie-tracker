package com.example.fitto.feature_recipes

data class RecipeShoppingLine(
    val raw: String,
    val name: String,
    val amountUnit: String,
    val amount: String = "",
    val unit: String = ""
)

class RecipeShoppingListController(
    initialItems: Collection<String> = emptyList()
) {

    private val mutableItems = initialItems
        .map(String::trim)
        .filter(String::isNotBlank)
        .toMutableList()

    val items: List<String>
        get() = mutableItems.toList()

    fun addItem(value: String): Boolean {
        val normalized = value.trim()
        if (normalized.isBlank()) return false
        mutableItems.add(normalized)
        return true
    }

    fun removeRawItem(raw: String): Boolean {
        return mutableItems.remove(raw)
    }

    fun updateItem(
        index: Int,
        name: String,
        amount: String,
        unit: String
    ): Boolean {
        if (index !in mutableItems.indices) return false
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return false

        val amountUnit = listOf(amount.trim(), unit.trim())
            .filter(String::isNotBlank)
            .joinToString(" ")
        mutableItems[index] = listOf(amountUnit, normalizedName)
            .filter(String::isNotBlank)
            .joinToString(" ")
        return true
    }

    fun lines(): List<RecipeShoppingLine> {
        return mutableItems.map(::parseLine)
    }

    fun lineAt(index: Int): RecipeShoppingLine? {
        return mutableItems.getOrNull(index)?.let(::parseLine)
    }

    fun exportText(title: String): String? {
        val cleanItems = mutableItems.map(String::trim).filter(String::isNotBlank)
        if (cleanItems.isEmpty()) return null
        return buildString {
            append(title).append('\n')
            cleanItems.forEach { item -> append("• ").append(item).append('\n') }
        }
    }

    private fun parseLine(value: String): RecipeShoppingLine {
        val tokens = value.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return RecipeShoppingLine(value, "", "")

        var nameStart = 0
        val amountParts = mutableListOf<String>()
        if (tokens.first().any(Char::isDigit) || tokens.first().contains('/')) {
            amountParts.add(tokens.first())
            nameStart = 1
            while (
                nameStart < tokens.size &&
                tokens[nameStart].lowercase() in UNIT_WORDS &&
                amountParts.size < MAX_AMOUNT_PARTS
            ) {
                amountParts.add(tokens[nameStart])
                nameStart += 1
            }
        }

        val name = tokens.drop(nameStart).joinToString(" ").ifBlank { value.trim() }
        val amountUnit = amountParts.joinToString(" ")
        return RecipeShoppingLine(
            raw = value,
            name = name,
            amountUnit = amountUnit,
            amount = amountParts.firstOrNull().orEmpty(),
            unit = amountParts.drop(1).joinToString(" ")
        )
    }

    private companion object {
        private const val MAX_AMOUNT_PARTS = 5
        private val UNIT_WORDS = setOf(
            "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
            "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
            "avuç", "paket", "kase", "diş"
        )
    }
}
