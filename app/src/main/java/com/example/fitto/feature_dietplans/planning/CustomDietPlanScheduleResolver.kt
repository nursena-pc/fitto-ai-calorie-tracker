package com.example.fitto.feature_dietplans.planning

import com.example.fitto.feature_meal.model.MealType
import java.util.Locale
import kotlin.math.roundToInt

object CustomDietPlanScheduleResolver {

    fun safeDuration(durationDays: Int): Int {
        return durationDays.coerceAtLeast(1)
    }

    fun normalizedDay(
        dayNumber: Int,
        durationDays: Int
    ): Int {
        val duration = safeDuration(durationDays)
        return ((dayNumber - 1).floorMod(duration)) + 1
    }

    fun mealTypesFor(
        mealOrder: List<String>,
        mealCount: Int
    ): List<MealType> {
        val selectedMeals = mealOrder
            .mapNotNull { value -> value.toMealTypeOrNull() }
            .distinct()

        if (selectedMeals.isNotEmpty()) {
            return selectedMeals
        }

        return when (mealCount.coerceIn(1, 4)) {
            1 -> listOf(MealType.DINNER)
            2 -> listOf(MealType.BREAKFAST, MealType.DINNER)
            3 -> listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
            else -> listOf(MealType.BREAKFAST, MealType.SNACK, MealType.LUNCH, MealType.DINNER)
        }
    }

    fun mealRatios(mealTypes: List<MealType>): Map<MealType, Double> {
        val total = mealTypes.sumOf(::baseMealRatio).coerceAtLeast(MIN_TOTAL_RATIO)

        return mealTypes.associateWith { mealType ->
            baseMealRatio(mealType) / total
        }
    }

    fun targetMealCalories(
        dailyCalories: Int,
        ratio: Double
    ): Int {
        return (dailyCalories * ratio)
            .roundToInt()
            .roundToNearest(ROUND_STEP)
            .coerceAtLeast(MIN_MEAL_CALORIES)
    }

    fun baseCalories(
        targetMealCalories: Int,
        dailyCalories: Int
    ): Int {
        return ((targetMealCalories.toDouble() / dailyCalories.coerceAtLeast(1)) *
            PlannedMealUiModel.BASELINE_CALORIES)
            .roundToInt()
            .roundToNearest(ROUND_STEP)
            .coerceAtLeast(MIN_MEAL_CALORIES)
    }

    private fun baseMealRatio(mealType: MealType): Double {
        return when (mealType) {
            MealType.BREAKFAST -> 0.25
            MealType.SNACK -> 0.15
            MealType.LUNCH -> 0.35
            MealType.DINNER -> 0.25
        }
    }

    private fun String.toMealTypeOrNull(): MealType? {
        return when (trim().lowercase(Locale.ROOT)) {
            MealType.BREAKFAST.argumentValue, "kahvaltı", "kahvalti", "breakfast" -> MealType.BREAKFAST
            MealType.SNACK.argumentValue, "ara öğün", "ara ogun", "atıştırmalık", "atistirmalik", "snack" -> MealType.SNACK
            MealType.LUNCH.argumentValue, "öğle", "ogle", "lunch" -> MealType.LUNCH
            MealType.DINNER.argumentValue, "akşam", "aksam", "dinner" -> MealType.DINNER
            else -> null
        }
    }

    private fun Int.floorMod(other: Int): Int {
        return ((this % other) + other) % other
    }

    private fun Int.roundToNearest(step: Int): Int {
        return ((this + step / 2) / step) * step
    }

    private const val MIN_TOTAL_RATIO = 0.01
    private const val MIN_MEAL_CALORIES = 60
    private const val ROUND_STEP = 5
}
