package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_meal.model.FoodUiModel
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import kotlin.math.roundToInt
import java.util.Locale
class AddMealFoodPresentationResolver(
    private val idSuffixProvider: () -> String = { System.nanoTime().toString() }
) {

    fun toFoodUiModel(
        food: FoodEntity,
        languageCode: String,
        isFavorite: Boolean = false
    ): FoodUiModel {
        val servingAmount = food.defaultServingAmount.takeIf { it > 0.0 } ?: DEFAULT_SERVING_AMOUNT
        val servingCalories = ((food.caloriesPer100g * servingAmount) / 100.0).roundToInt()

        return FoodUiModel(
            id = food.id,
            name = localizedFoodName(food, languageCode),
            serving = formatServingLabel(servingAmount, food.defaultUnit),
            calories = servingCalories.coerceAtLeast(0),
            emoji = foodEmoji(food),
            defaultServingAmount = servingAmount,
            defaultUnit = food.defaultUnit,
            caloriesPer100g = food.caloriesPer100g,
            proteinPer100g = food.proteinPer100g,
            carbsPer100g = food.carbsPer100g,
            fatPer100g = food.fatPer100g,
            isFavorite = isFavorite
        )
    }

    fun toSelectedFoodFromDetectedServing(food: FoodEntity): SelectedFoodUiModel {
        val servingAmount = food.defaultServingAmount
            .takeIf { it > 0.0 }
            ?: DEFAULT_SERVING_AMOUNT
        val unit = food.defaultUnit.ifBlank { DEFAULT_UNIT }
        val multiplier = servingAmount / 100.0

        return SelectedFoodUiModel(
            id = "${food.id}-${idSuffixProvider()}",
            foodId = food.id,
            name = food.nameTr,
            amountLabel = formatServingLabel(servingAmount, unit),
            amountGrams = servingAmount.roundOneDecimal(),
            calories = (food.caloriesPer100g * multiplier).roundToInt().coerceAtLeast(0),
            protein = (food.proteinPer100g * multiplier).roundOneDecimal(),
            carbs = (food.carbsPer100g * multiplier).roundOneDecimal(),
            fat = (food.fatPer100g * multiplier).roundOneDecimal(),
            emoji = foodEmoji(food)
        )
    }

    fun localizedFoodName(food: FoodEntity, languageCode: String): String {
        return if (languageCode == ENGLISH_LANGUAGE_CODE) {
            food.nameEn?.takeIf { it.isNotBlank() } ?: food.nameTr
        } else {
            food.nameTr
        }
    }

    fun formatServingLabel(amount: Double, unit: String): String {
        val normalizedAmount = if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", amount)
        }
        return "$normalizedAmount $unit"
    }

    fun foodEmoji(food: FoodEntity): String {
        return when {
            food.id.contains("oat", ignoreCase = true) || food.category == "grain" -> "🥣"
            food.id.contains("chicken", ignoreCase = true) ||
                    food.id.contains("turkey", ignoreCase = true) -> "🍗"

            food.id.contains("egg", ignoreCase = true) -> "🥚"
            food.id.contains("fish", ignoreCase = true) ||
                    food.id.contains("tuna", ignoreCase = true) -> "🐟"

            food.id.contains("yogurt", ignoreCase = true) ||
                    food.id.contains("milk", ignoreCase = true) ||
                    food.category == "dairy" -> "🥛"

            food.id.contains("banana", ignoreCase = true) -> "🍌"
            food.id.contains("apple", ignoreCase = true) -> "🍎"
            food.id.contains("orange", ignoreCase = true) -> "🍊"
            food.id.contains("strawberry", ignoreCase = true) -> "🍓"
            food.category == "fruit" -> "🍏"
            food.category == "vegetable" -> "🥗"
            food.category == "legume" -> "🫘"
            food.category == "nut" || food.category == "oil" -> "🥜"
            food.category == "soup" -> "🍲"
            else -> "🍽️"
        }
    }

    private fun Double.roundOneDecimal(): Double {
        return (this * 10.0).roundToInt() / 10.0
    }

    private companion object {
        private const val DEFAULT_SERVING_AMOUNT = 100.0
        private const val DEFAULT_UNIT = "g"
        private const val ENGLISH_LANGUAGE_CODE = "en"
    }
}
