package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard

interface RecipeShoppingListStore {
    fun items(): List<String>
    fun replaceItems(items: Collection<String>)
    fun addItems(items: Collection<String>)
}

class SharedPreferencesRecipeShoppingListStore(
    context: Context
) : RecipeShoppingListStore {

    private val appContext = context.applicationContext

    override fun items(): List<String> {
        return preferences()
            .getStringSet(SHOPPING_ITEMS_KEY, emptySet())
            .orEmpty()
            .map(String::trim)
            .filter(String::isNotBlank)
            .sorted()
    }

    override fun replaceItems(items: Collection<String>) {
        preferences()
            .edit()
            .putStringSet(SHOPPING_ITEMS_KEY, normalizedItems(items))
            .apply()
    }

    override fun addItems(items: Collection<String>) {
        if (items.isEmpty()) return
        val updatedItems = this.items().toMutableSet().apply { addAll(items) }
        replaceItems(updatedItems)
    }

    private fun preferences() = UserSessionDataGuard.scopedSharedPreferences(
        appContext,
        SHOPPING_PREFS
    )

    private fun normalizedItems(items: Collection<String>): Set<String> {
        return items.map(String::trim).filter(String::isNotBlank).toSet()
    }

    private companion object {
        private const val SHOPPING_PREFS = "fitto_recipe_shopping_list"
        private const val SHOPPING_ITEMS_KEY = "items"
    }
}
