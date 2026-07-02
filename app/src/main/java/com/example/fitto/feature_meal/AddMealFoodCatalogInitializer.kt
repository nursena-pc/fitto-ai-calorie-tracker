package com.example.fitto.feature_meal

internal class AddMealFoodCatalogInitializer(
    private val importFoodSeed: suspend () -> Unit
) {
    private var initialized = false

    suspend fun ensureInitialized() {
        if (initialized) return

        importFoodSeed()
        initialized = true
    }
}
