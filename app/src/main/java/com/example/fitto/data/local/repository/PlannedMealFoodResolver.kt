package com.example.fitto.data.local.repository

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.entity.MealEntryFoodEntity
import com.example.fitto.data.local.entity.PlannedMealFoodEntity
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel
import java.util.Locale

internal data class PlannedMealFoodRequest(
    val title: String,
    val targetCalories: Int,
    val plannedMealCalories: Int,
    val suggestedFoodNames: List<String>
)

internal class PlannedMealFoodResolver(
    private val payloadFactory: MealEntryPayloadFactory
) {

    fun resolve(
        request: PlannedMealFoodRequest,
        mealEntryId: String,
        createdAt: Long,
        allFoods: List<FoodEntity>,
        plannedFoods: List<PlannedMealFoodEntity>
    ): List<MealEntryFoodEntity> {
        val lookup = buildFoodLookup(allFoods)

        val rowsFromPlan = plannedFoods.mapNotNull { plannedFood ->
            val food = resolveFood(
                rawKey = plannedFood.foodId,
                allFoods = allFoods,
                lookup = lookup
            ) ?: return@mapNotNull null

            payloadFactory.createPlannedFoodRow(
                mealEntryId = mealEntryId,
                food = food,
                amountGrams = plannedFood.scaledAmountGrams(request.targetCalories),
                createdAt = createdAt
            )
        }

        if (rowsFromPlan.isNotEmpty()) return rowsFromPlan

        val rowsFromSuggestions = resolveSuggestedFoods(
            suggestedFoodNames = request.suggestedFoodNames,
            plannedMealCalories = request.plannedMealCalories,
            mealEntryId = mealEntryId,
            createdAt = createdAt,
            allFoods = allFoods,
            lookup = lookup
        )

        if (rowsFromSuggestions.isNotEmpty()) return rowsFromSuggestions

        return listOf(
            payloadFactory.createEstimatedPlannedFoodRow(
                mealEntryId = mealEntryId,
                title = request.title,
                calories = request.plannedMealCalories,
                createdAt = createdAt
            )
        )
    }

    private fun resolveSuggestedFoods(
        suggestedFoodNames: List<String>,
        plannedMealCalories: Int,
        mealEntryId: String,
        createdAt: Long,
        allFoods: List<FoodEntity>,
        lookup: Map<String, FoodEntity>
    ): List<MealEntryFoodEntity> {
        val matchedFoods = suggestedFoodNames
            .mapNotNull { name ->
                resolveFood(
                    rawKey = name,
                    allFoods = allFoods,
                    lookup = lookup
                )
            }
            .distinctBy(FoodEntity::id)

        if (matchedFoods.isEmpty()) return emptyList()

        val baseRows = matchedFoods.map { food ->
            payloadFactory.createPlannedFoodRow(
                mealEntryId = mealEntryId,
                food = food,
                amountGrams = food.defaultServingAmount.takeIf { it > 0.0 } ?: DEFAULT_SERVING_GRAMS,
                createdAt = createdAt
            )
        }

        val baseCalories = baseRows.sumOf(MealEntryFoodEntity::calories).coerceAtLeast(1)
        val scale = plannedMealCalories.toDouble() / baseCalories.toDouble()

        return matchedFoods.map { food ->
            val baseAmount = food.defaultServingAmount.takeIf { it > 0.0 } ?: DEFAULT_SERVING_GRAMS
            payloadFactory.createPlannedFoodRow(
                mealEntryId = mealEntryId,
                food = food,
                amountGrams = (baseAmount * scale).coerceIn(MIN_AMOUNT_GRAMS, MAX_AMOUNT_GRAMS),
                createdAt = createdAt
            )
        }
    }

    private fun buildFoodLookup(foods: List<FoodEntity>): Map<String, FoodEntity> {
        return buildMap {
            foods.forEach { food ->
                put(normalizeFoodKey(food.id), food)
                put(normalizeFoodKey(food.nameTr), food)
                food.nameEn?.takeIf { it.isNotBlank() }?.let { nameEn ->
                    put(normalizeFoodKey(nameEn), food)
                }
            }
        }
    }

    private fun resolveFood(
        rawKey: String,
        allFoods: List<FoodEntity>,
        lookup: Map<String, FoodEntity>
    ): FoodEntity? {
        val normalized = normalizeFoodKey(rawKey)

        lookup[normalized]?.let { return it }
        lookup[normalizeFoodKey(rawKey.substringAfterLast('/'))]?.let { return it }
        lookup[normalizeFoodKey(rawKey.replace("-", " "))]?.let { return it }
        lookup[normalizeFoodKey(rawKey.replace("_", " "))]?.let { return it }

        if (normalized.length < MIN_FUZZY_KEY_LENGTH) return null

        return allFoods.firstOrNull { food ->
            val foodNameTr = normalizeFoodKey(food.nameTr)
            val foodNameEn = normalizeFoodKey(food.nameEn.orEmpty())

            foodNameTr.contains(normalized) ||
                    normalized.contains(foodNameTr) ||
                    foodNameEn.contains(normalized) ||
                    normalized.contains(foodNameEn)
        }
    }

    private fun normalizeFoodKey(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    private fun PlannedMealFoodEntity.scaledAmountGrams(targetCalories: Int): Double {
        if (!scalable) return amountGrams

        val safeTargetCalories = targetCalories.coerceIn(
            PlannedMealUiModel.MIN_TARGET_CALORIES,
            PlannedMealUiModel.MAX_TARGET_CALORIES
        )
        val scale = safeTargetCalories.toDouble() / PlannedMealUiModel.BASELINE_CALORIES.toDouble()

        return (amountGrams * scale).coerceIn(minAmountGrams, maxAmountGrams)
    }

    private companion object {
        const val DEFAULT_SERVING_GRAMS = 100.0
        const val MIN_AMOUNT_GRAMS = 1.0
        const val MAX_AMOUNT_GRAMS = 5000.0
        const val MIN_FUZZY_KEY_LENGTH = 4
    }
}
