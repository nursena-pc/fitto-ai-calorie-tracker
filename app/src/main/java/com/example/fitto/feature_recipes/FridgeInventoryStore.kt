package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import org.json.JSONArray
import org.json.JSONObject

interface FridgeInventoryStore {
    fun ingredients(): List<FridgeIngredient>
    fun replaceIngredients(ingredients: Collection<FridgeIngredient>)
}

class SharedPreferencesFridgeInventoryStore(
    context: Context
) : FridgeInventoryStore {

    private val appContext = context.applicationContext

    override fun ingredients(): List<FridgeIngredient> {
        val raw = preferences().getString(FRIDGE_ITEMS_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                FridgeIngredient(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    amount = item.optString("amount"),
                    unit = item.optString("unit")
                )
            }.filter { it.name.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    override fun replaceIngredients(ingredients: Collection<FridgeIngredient>) {
        val array = JSONArray()
        ingredients.forEach { ingredient ->
            array.put(JSONObject().apply {
                put("id", ingredient.id)
                put("name", ingredient.name)
                put("amount", ingredient.amount)
                put("unit", ingredient.unit)
            })
        }
        preferences().edit().putString(FRIDGE_ITEMS_KEY, array.toString()).apply()
    }

    private fun preferences() = UserSessionDataGuard.scopedSharedPreferences(
        appContext,
        FRIDGE_PREFS
    )

    private companion object {
        private const val FRIDGE_PREFS = "fitto_fridge_inventory"
        private const val FRIDGE_ITEMS_KEY = "items"
    }
}
