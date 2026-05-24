package com.example.fitto.feature_recipes.model

/**
 * Tarif ana ekranının üst bölümünü tek ve küçük bir state ile yönetir.
 * Bu state, liste adapterından bağımsızdır; böylece tarif kartları yenilenirken header zıplamaz.
 */
data class RecipeHeaderState(
    val filterState: RecipeFilterState = RecipeFilterState(),
    val searchQuery: String = "",
    val resultCount: Int = 0,
    val listMode: RecipeListMode = RecipeListMode.GRID,
    val notificationBadgeCount: Int = 0
)
