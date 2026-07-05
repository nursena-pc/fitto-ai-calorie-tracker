package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard

interface RecipeShoppingListStore {
    fun addItems(items: Collection<String>)
}

class SharedPreferencesRecipeShoppingListStore(
    context: Context
) : RecipeShoppingListStore {

    private val appContext = context.applicationContext

    override fun addItems(items: Collection<String>) {
        if (items.isEmpty()) return
        val preferences = UserSessionDataGuard.scopedSharedPreferences(appContext, SHOPPING_PREFS)
        val current = preferences.getStringSet(SHOPPING_ITEMS_KEY, emptySet()).orEmpty().toMutableSet()
        current.addAll(items)
        preferences.edit().putStringSet(SHOPPING_ITEMS_KEY, current).apply()
    }

    private companion object {
        private const val SHOPPING_PREFS = "fitto_recipe_shopping_list"
        private const val SHOPPING_ITEMS_KEY = "items"
    }
}
