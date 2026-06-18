package com.example.fitto.data.local.repository

import android.content.Context
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.session.UserSessionDataGuard
import kotlinx.coroutines.flow.Flow
import com.example.fitto.data.local.entity.FavoriteFoodEntity
import com.example.fitto.data.remote.usda.UsdaFoodDataCentralClient
import com.example.fitto.data.remote.usda.UsdaFoodTranslationDictionary
import com.example.fitto.BuildConfig
import com.example.fitto.data.remote.logmeal.LogMealFoodCandidate
import java.util.UUID

class FoodRepository(context: Context) {

    private val appContext = context.applicationContext
    private val usdaClient = UsdaFoodDataCentralClient(BuildConfig.USDA_API_KEY)
    private val database = FittoDatabaseProvider.getDatabase(appContext)
    private val mealDao = database.mealDao()
    private val foodDao = database.foodDao()
    private val favoriteFoodDao = database.favoriteFoodDao()

    private fun currentUserId(): String {
        return UserSessionDataGuard.getActiveUid(appContext) ?: LOCAL_ANONYMOUS_USER_ID
    }

    fun observeAllFoods(): Flow<List<FoodEntity>> = foodDao.observeAllFoods(currentUserId())

    suspend fun getFavoriteFoodIds(limit: Int = 50): List<String> {
        return favoriteFoodDao.getFavoriteFoodIds(currentUserId(), limit)
    }

    suspend fun getFavoriteFoods(limit: Int = 50): List<FoodEntity> {
        val userId = currentUserId()
        val favoriteIds = favoriteFoodDao.getFavoriteFoodIds(userId, limit)
        if (favoriteIds.isEmpty()) return emptyList()

        val foods = foodDao.getFoodsByIds(userId, favoriteIds)
        val foodsById = foods.associateBy { it.id }

        return favoriteIds.mapNotNull { foodId ->
            foodsById[foodId]
        }
    }
    suspend fun createFoodFromLogMealCameraResult(
        candidate: LogMealFoodCandidate,
        mealType: String
    ): FoodEntity {
        val now = System.currentTimeMillis()

        val servingGrams = candidate.servingGrams
            ?.takeIf { it > 0.0 }
            ?.coerceIn(1.0, 2000.0)
            ?: 100.0

        val factorTo100g = 100.0 / servingGrams

        val userId = currentUserId()

        val food = FoodEntity(
            id = "logmeal_${now}_${UUID.randomUUID()}",
            nameTr = candidate.name.trim().ifBlank { "Kamera ile tanınan besin" },
            nameEn = candidate.name.trim().ifBlank { "Camera recognized food" },
            category = "camera_food",
            defaultUnit = "g",
            defaultServingAmount = servingGrams,
            caloriesPer100g = ((candidate.calories ?: 0.0) * factorTo100g).coerceIn(0.0, 1000.0),
            proteinPer100g = ((candidate.protein ?: 0.0) * factorTo100g).coerceIn(0.0, 100.0),
            carbsPer100g = ((candidate.carbs ?: 0.0) * factorTo100g).coerceIn(0.0, 100.0),
            fatPer100g = ((candidate.fat ?: 0.0) * factorTo100g).coerceIn(0.0, 100.0),
            fiberPer100g = candidate.fiber?.let { (it * factorTo100g).coerceIn(0.0, 100.0) },
            source = "logmeal_camera",
            isVerified = false,
            allergens = "",
            dietTags = "logmeal_camera",
            mealTypes = mealType,
            ownerUserId = userId,
            createdAt = now,
            updatedAt = now
        )

        foodDao.upsertFoods(listOf(food))
        return food
    }

    suspend fun isFavoriteFood(foodId: String): Boolean {
        return favoriteFoodDao.countFavorite(currentUserId(), foodId) > 0
    }

    suspend fun toggleFavoriteFood(foodId: String): Boolean {
        val userId = currentUserId()
        val isFavorite = favoriteFoodDao.countFavorite(userId, foodId) > 0

        return if (isFavorite) {
            favoriteFoodDao.removeFavorite(userId, foodId)
            false
        } else {
            favoriteFoodDao.upsertFavorite(
                FavoriteFoodEntity(
                    id = "${userId}_$foodId",
                    userId = userId,
                    foodId = foodId,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun searchFoods(
        query: String,
        mealType: String? = null,
        limit: Int = 40
    ): List<FoodEntity> {
        val trimmedQuery = query.trim()
        val userId = currentUserId()

        if (trimmedQuery.isNotBlank()) {
            return foodDao.searchFoods(userId = userId, query = trimmedQuery, limit = limit)
        }

        return if (mealType.isNullOrBlank()) {
            foodDao.searchFoods(userId = userId, query = "", limit = limit)
        } else {
            foodDao.getFoodsByMealType(userId = userId, mealType = mealType, limit = limit)
        }
    }
    suspend fun createCustomFood(
        name: String,
        defaultServingAmount: Double,
        defaultUnit: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        mealType: String
    ): FoodEntity {
        val now = System.currentTimeMillis()
        val safeName = name.trim()

        val userId = currentUserId()

        val food = FoodEntity(
            id = "user_custom_${now}_${safeName.lowercase().replace(" ", "_")}",
            nameTr = safeName,
            nameEn = null,
            category = "user_custom",
            defaultUnit = defaultUnit.ifBlank { "g" },
            defaultServingAmount = defaultServingAmount.coerceIn(1.0, 5000.0),
            caloriesPer100g = caloriesPer100g.coerceIn(0.0, 1000.0),
            proteinPer100g = proteinPer100g.coerceIn(0.0, 100.0),
            carbsPer100g = carbsPer100g.coerceIn(0.0, 100.0),
            fatPer100g = fatPer100g.coerceIn(0.0, 100.0),
            fiberPer100g = null,
            source = "user_custom",
            isVerified = false,
            allergens = "",
            dietTags = "custom",
            mealTypes = mealType,
            ownerUserId = userId,
            createdAt = now,
            updatedAt = now
        )

        foodDao.upsertFood(food)
        return food
    }
    suspend fun getRecentFoods(limit: Int = 12): List<FoodEntity> {
        val userId = currentUserId()
        val recentFoodIds = mealDao.getRecentFoodIds(userId, limit)
        if (recentFoodIds.isEmpty()) return emptyList()

        val foods = foodDao.getFoodsByIds(userId, recentFoodIds)
        val foodsById = foods.associateBy { it.id }

        return recentFoodIds.mapNotNull { foodId ->
            foodsById[foodId]
        }
    }
    suspend fun searchUsdaFoods(
        query: String,
        mealType: String,
        limit: Int = 8,
        pageNumber: Int = 1
    ): List<FoodEntity> {
        val usdaQuery = UsdaFoodTranslationDictionary.translateQueryForUsda(query)

        android.util.Log.d(
            "FoodRepository",
            "USDA query translated: '$query' -> '$usdaQuery', page=$pageNumber"
        )

        val products = usdaClient.searchFoods(
            query = usdaQuery,
            pageSize = limit,
            pageNumber = pageNumber
        )

        android.util.Log.d("FoodRepository", "USDA products for '$usdaQuery': ${products.size}")

        if (products.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()

        val foods = products.map { product ->
            val translatedTurkishName = UsdaFoodTranslationDictionary
                .translateDescriptionToTurkish(product.description)

            val simplifiedEnglishName = UsdaFoodTranslationDictionary
                .simplifyEnglishDescription(product.description)

            FoodEntity(
                id = "usda_${product.fdcId}",
                nameTr = buildString {
                    append(translatedTurkishName)

                    product.brandOwner
                        ?.takeIf { it.isNotBlank() }
                        ?.let { brand ->
                            append(" · ")
                            append(brand)
                        }
                },
                nameEn = buildString {
                    append(simplifiedEnglishName)

                    product.brandOwner
                        ?.takeIf { it.isNotBlank() }
                        ?.let { brand ->
                            append(" · ")
                            append(brand)
                        }
                },
                category = if (product.dataType == "Branded") {
                    "packaged_product"
                } else {
                    "usda_food"
                },
                defaultUnit = "g",
                defaultServingAmount = 100.0,
                caloriesPer100g = product.caloriesPer100g.coerceIn(0.0, 1000.0),
                proteinPer100g = product.proteinPer100g.coerceIn(0.0, 100.0),
                carbsPer100g = product.carbsPer100g.coerceIn(0.0, 100.0),
                fatPer100g = product.fatPer100g.coerceIn(0.0, 100.0),
                fiberPer100g = product.fiberPer100g?.coerceIn(0.0, 100.0),
                source = "usda_fdc",
                isVerified = product.dataType != "Branded",
                allergens = "",
                dietTags = "usda_fdc",
                mealTypes = mealType,
                createdAt = now,
                updatedAt = now
            )
        }

        foodDao.upsertFoods(foods)
        return foods
    }


    suspend fun getFoodById(foodId: String): FoodEntity? {
        return foodDao.getFoodById(currentUserId(), foodId)
    }

    companion object {
        private const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
    }

}
