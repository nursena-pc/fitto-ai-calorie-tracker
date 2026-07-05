package com.example.fitto.data.local.repository

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.entity.MealEntryEntity
import com.example.fitto.data.local.entity.MealEntryFoodEntity
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

internal data class MealEntryPayload(
    val entry: MealEntryEntity,
    val foods: List<MealEntryFoodEntity>
)

internal class MealEntryPayloadFactory(
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis
) {

    fun createManual(
        userId: String,
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String
    ): MealEntryPayload? {
        if (selectedFoods.isEmpty()) return null

        val now = clock()
        val mealEntryId = idFactory()
        val foodRows = selectedFoods.map { selectedFood ->
            selectedFood.toMealEntryFoodEntity(
                mealEntryId = mealEntryId,
                createdAt = now
            )
        }

        return MealEntryPayload(
            entry = MealEntryEntity(
                id = mealEntryId,
                userId = userId,
                entryDate = entryDate,
                mealType = mealType.argumentValue,
                title = selectedFoods.toMealTitle(),
                totalCalories = foodRows.sumOf { it.calories },
                totalProtein = foodRows.sumOf { it.protein },
                totalCarbs = foodRows.sumOf { it.carbs },
                totalFat = foodRows.sumOf { it.fat },
                sourceType = MealEntryEntity.SOURCE_MANUAL,
                sourcePlanId = null,
                sourcePlannedMealId = null,
                createdAt = now,
                updatedAt = now
            ),
            foods = foodRows
        )
    }

    fun createPlanned(
        userId: String,
        mealType: MealType,
        entryDate: String,
        planId: String,
        plannedMealId: String,
        mealEntryId: String,
        createdAt: Long,
        foodRows: List<MealEntryFoodEntity>
    ): MealEntryPayload {
        return MealEntryPayload(
            entry = MealEntryEntity(
                id = mealEntryId,
                userId = userId,
                entryDate = entryDate,
                mealType = mealType.argumentValue,
                title = foodRows.toMealTitleFromRows(),
                totalCalories = foodRows.sumOf { it.calories },
                totalProtein = foodRows.sumOf { it.protein }.roundOneDecimal(),
                totalCarbs = foodRows.sumOf { it.carbs }.roundOneDecimal(),
                totalFat = foodRows.sumOf { it.fat }.roundOneDecimal(),
                sourceType = MealEntryEntity.SOURCE_DIET_PLAN,
                sourcePlanId = planId,
                sourcePlannedMealId = plannedMealId,
                createdAt = createdAt,
                updatedAt = createdAt
            ),
            foods = foodRows
        )
    }

    fun createPlannedFoodRow(
        mealEntryId: String,
        food: FoodEntity,
        amountGrams: Double,
        createdAt: Long
    ): MealEntryFoodEntity {
        val safeAmount = amountGrams.coerceAtLeast(1.0)

        return MealEntryFoodEntity(
            id = idFactory(),
            mealEntryId = mealEntryId,
            foodId = food.id,
            foodName = food.nameTr,
            amountLabel = formatAmountLabel(safeAmount, food.defaultUnit.ifBlank { "g" }),
            amountGrams = safeAmount,
            calories = ((food.caloriesPer100g * safeAmount) / 100.0).roundToInt().coerceAtLeast(0),
            protein = ((food.proteinPer100g * safeAmount) / 100.0).roundOneDecimal(),
            carbs = ((food.carbsPer100g * safeAmount) / 100.0).roundOneDecimal(),
            fat = ((food.fatPer100g * safeAmount) / 100.0).roundOneDecimal(),
            createdAt = createdAt
        )
    }

    fun createEstimatedPlannedFoodRow(
        mealEntryId: String,
        title: String,
        calories: Int,
        createdAt: Long
    ): MealEntryFoodEntity {
        val safeCalories = calories.coerceAtLeast(0)

        return MealEntryFoodEntity(
            id = idFactory(),
            mealEntryId = mealEntryId,
            foodId = null,
            foodName = title,
            amountLabel = "Planlı öğün",
            amountGrams = 0.0,
            calories = safeCalories,
            protein = (safeCalories * 0.20 / 4.0).roundOneDecimal(),
            carbs = (safeCalories * 0.50 / 4.0).roundOneDecimal(),
            fat = (safeCalories * 0.30 / 9.0).roundOneDecimal(),
            createdAt = createdAt
        )
    }

    fun updateFoodAmount(
        foodRow: MealEntryFoodEntity,
        newAmount: Double
    ): MealEntryFoodEntity? {
        val oldAmount = foodRow.amountGrams.takeIf { it > 0.0 } ?: return null
        val safeAmount = newAmount.coerceIn(MIN_AMOUNT_GRAMS, MAX_AMOUNT_GRAMS)
        val multiplier = safeAmount / oldAmount

        return foodRow.copy(
            amountGrams = safeAmount.roundOneDecimal(),
            amountLabel = safeAmount.formatAmount(foodRow.amountLabel.extractUnitOrDefault()),
            calories = (foodRow.calories * multiplier).roundToInt().coerceAtLeast(0),
            protein = (foodRow.protein * multiplier).roundOneDecimal(),
            carbs = (foodRow.carbs * multiplier).roundOneDecimal(),
            fat = (foodRow.fat * multiplier).roundOneDecimal()
        )
    }

    fun scaleFoods(
        foods: List<MealEntryFoodEntity>,
        multiplier: Double
    ): List<MealEntryFoodEntity> {
        val safeMultiplier = multiplier.coerceIn(MIN_ENTRY_SCALE, MAX_ENTRY_SCALE)

        return foods.map { food ->
            val updatedAmount = (food.amountGrams * safeMultiplier).roundOneDecimal()
            food.copy(
                amountGrams = updatedAmount,
                amountLabel = updatedAmount.formatAmount(food.amountLabel.extractUnitOrDefault()),
                calories = (food.calories * safeMultiplier).roundToInt().coerceAtLeast(0),
                protein = (food.protein * safeMultiplier).roundOneDecimal(),
                carbs = (food.carbs * safeMultiplier).roundOneDecimal(),
                fat = (food.fat * safeMultiplier).roundOneDecimal()
            )
        }
    }

    fun recalculateEntry(
        entry: MealEntryEntity,
        foods: List<MealEntryFoodEntity>,
        updatedAt: Long = clock()
    ): MealEntryEntity {
        return entry.copy(
            title = foods.toMealTitleFromRows(),
            totalCalories = foods.sumOf { it.calories },
            totalProtein = foods.sumOf { it.protein }.roundOneDecimal(),
            totalCarbs = foods.sumOf { it.carbs }.roundOneDecimal(),
            totalFat = foods.sumOf { it.fat }.roundOneDecimal(),
            updatedAt = updatedAt
        )
    }

    private fun SelectedFoodUiModel.toMealEntryFoodEntity(
        mealEntryId: String,
        createdAt: Long
    ): MealEntryFoodEntity {
        return MealEntryFoodEntity(
            id = idFactory(),
            mealEntryId = mealEntryId,
            foodId = foodId,
            foodName = name,
            amountLabel = amountLabel,
            amountGrams = amountGrams.roundOneDecimal(),
            calories = calories,
            protein = protein.roundOneDecimal(),
            carbs = carbs.roundOneDecimal(),
            fat = fat.roundOneDecimal(),
            createdAt = createdAt
        )
    }

    private fun List<SelectedFoodUiModel>.toMealTitle(): String {
        val names = take(TITLE_FOOD_LIMIT).joinToString(separator = ", ") { it.name }
        return if (size > TITLE_FOOD_LIMIT) "$names +${size - TITLE_FOOD_LIMIT}" else names
    }

    private fun List<MealEntryFoodEntity>.toMealTitleFromRows(): String {
        val names = take(TITLE_FOOD_LIMIT).joinToString(separator = ", ") { it.foodName }
        return if (size > TITLE_FOOD_LIMIT) "$names +${size - TITLE_FOOD_LIMIT}" else names
    }

    private fun formatAmountLabel(amount: Double, unit: String): String {
        val amountText = if (amount % 1.0 == 0.0) {
            amount.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.1f", amount)
        }

        return "$amountText $unit"
    }

    private fun Double.formatAmount(unit: String): String {
        val safeUnit = if (unit.isBlank()) "g" else unit
        return "${roundToInt()} $safeUnit"
    }

    private fun String.extractUnitOrDefault(): String {
        val unit = trim().substringAfterLast(' ', missingDelimiterValue = "g").trim()
        return unit.takeIf { it.isNotBlank() && it.any { char -> char.isLetter() } } ?: "g"
    }

    private fun Double.roundOneDecimal(): Double = (this * 10.0).roundToInt() / 10.0

    private companion object {
        const val TITLE_FOOD_LIMIT = 2
        const val MIN_AMOUNT_GRAMS = 1.0
        const val MAX_AMOUNT_GRAMS = 5000.0
        const val MIN_ENTRY_SCALE = 0.5
        const val MAX_ENTRY_SCALE = 2.0
    }
}
