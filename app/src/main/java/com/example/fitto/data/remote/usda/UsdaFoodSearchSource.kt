package com.example.fitto.data.remote.usda

interface UsdaFoodSearchSource {
    fun searchFoods(
        query: String,
        pageSize: Int = 8,
        pageNumber: Int = 1
    ): List<UsdaFoodProduct>
}

internal interface UsdaFoodSearchTransport {
    fun search(
        query: String,
        pageSize: Int,
        pageNumber: Int
    ): String?
}
