package com.example.fitto.feature_recipes

import java.util.Locale

data class FridgeIngredient(
    val id: String,
    val name: String,
    val amount: String,
    val unit: String
)

data class FridgeIngredientInput(
    val name: String,
    val amount: String,
    val unit: String
)

class FridgeInventoryController(
    initialIngredients: Collection<FridgeIngredient>,
    private val idFactory: () -> String = { "fridge_${System.currentTimeMillis()}" }
) {

    private val mutableIngredients = initialIngredients
        .filter { it.name.isNotBlank() }
        .ifEmpty { defaultIngredients() }
        .toMutableList()

    private var searchQuery: String = ""

    val ingredients: List<FridgeIngredient>
        get() = mutableIngredients.toList()

    fun updateSearchQuery(query: String): List<FridgeIngredient> {
        searchQuery = query.trim()
        return visibleIngredients()
    }

    fun visibleIngredients(): List<FridgeIngredient> {
        val normalizedQuery = searchQuery.lowercase(TURKISH_LOCALE)
        return mutableIngredients.filter { ingredient ->
            normalizedQuery.isBlank() ||
                    ingredient.name.lowercase(TURKISH_LOCALE).contains(normalizedQuery)
        }
    }

    fun upsertIngredient(
        existingId: String?,
        input: FridgeIngredientInput
    ): FridgeIngredient? {
        val normalizedName = input.name.trim()
        if (normalizedName.isBlank()) return null

        val ingredient = FridgeIngredient(
            id = existingId ?: idFactory(),
            name = normalizedName,
            amount = input.amount.trim(),
            unit = input.unit.trim()
        )
        if (existingId == null) {
            mutableIngredients.add(ingredient)
        } else {
            val existingIndex = mutableIngredients.indexOfFirst { it.id == existingId }
            if (existingIndex < 0) return null
            mutableIngredients[existingIndex] = ingredient
        }
        return ingredient
    }

    fun removeIngredient(id: String): Boolean {
        return mutableIngredients.removeAll { it.id == id }
    }

    companion object {
        private val TURKISH_LOCALE = Locale("tr", "TR")

        fun defaultIngredients(): List<FridgeIngredient> = listOf(
            FridgeIngredient("default_tomato", "Domates", "4", "adet"),
            FridgeIngredient("default_egg", "Yumurta", "6", "adet"),
            FridgeIngredient("default_milk", "Süt", "1", "L"),
            FridgeIngredient("default_carrot", "Havuç", "2", "adet"),
            FridgeIngredient("default_yogurt", "Yoğurt", "2", "kase"),
            FridgeIngredient("default_potato", "Patates", "3", "adet")
        )
    }
}
