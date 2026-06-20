package com.example.fitto.feature_daily

import kotlin.math.roundToInt

object DailyNutritionTargetResolver {

    data class Targets(
        val dailyCalories: Int,
        val proteinGrams: Int,
        val carbGrams: Int,
        val fatGrams: Int
    )

    fun resolveTargets(
        activeCustomPlanCalories: Int?,
        profileDailyCalories: Int?,
        storedDailyCalories: Int,
        defaultDailyCalories: Int,
        profileProteinGrams: Int?,
        defaultProteinGrams: Int,
        profileCarbGrams: Int?,
        defaultCarbGrams: Int,
        profileFatGrams: Int?,
        defaultFatGrams: Int
    ): Targets {
        return Targets(
            dailyCalories = resolveDailyCalories(
                activeCustomPlanCalories = activeCustomPlanCalories,
                profileDailyCalories = profileDailyCalories,
                storedDailyCalories = storedDailyCalories,
                defaultDailyCalories = defaultDailyCalories
            ),
            proteinGrams = resolveProteinGrams(
                activeCustomPlanCalories = activeCustomPlanCalories,
                profileProteinGrams = profileProteinGrams,
                defaultProteinGrams = defaultProteinGrams
            ),
            carbGrams = resolveCarbGrams(
                activeCustomPlanCalories = activeCustomPlanCalories,
                profileCarbGrams = profileCarbGrams,
                defaultCarbGrams = defaultCarbGrams
            ),
            fatGrams = resolveFatGrams(
                activeCustomPlanCalories = activeCustomPlanCalories,
                profileFatGrams = profileFatGrams,
                defaultFatGrams = defaultFatGrams
            )
        )
    }

    fun resolveDailyCalories(
        activeCustomPlanCalories: Int?,
        profileDailyCalories: Int?,
        storedDailyCalories: Int,
        defaultDailyCalories: Int
    ): Int {
        return activeCustomPlanCalories?.takeIf { it > 0 }
            ?: profileDailyCalories?.takeIf { it > 0 }
            ?: storedDailyCalories.takeIf { it > 0 }
            ?: defaultDailyCalories
    }

    fun resolveProteinGrams(
        activeCustomPlanCalories: Int?,
        profileProteinGrams: Int?,
        defaultProteinGrams: Int
    ): Int {
        return activeCustomPlanCalories
            ?.takeIf { it > 0 }
            ?.let { calories ->
                gramsFromCalories(
                    calories = calories,
                    calorieRatio = PROTEIN_CALORIE_RATIO,
                    caloriesPerGram = CALORIES_PER_PROTEIN_GRAM,
                    minimumGrams = MIN_PROTEIN_GRAMS
                )
            }
            ?: profileProteinGrams?.takeIf { it > 0 }
            ?: defaultProteinGrams
    }

    fun resolveCarbGrams(
        activeCustomPlanCalories: Int?,
        profileCarbGrams: Int?,
        defaultCarbGrams: Int
    ): Int {
        return activeCustomPlanCalories
            ?.takeIf { it > 0 }
            ?.let { calories ->
                gramsFromCalories(
                    calories = calories,
                    calorieRatio = CARB_CALORIE_RATIO,
                    caloriesPerGram = CALORIES_PER_CARB_GRAM,
                    minimumGrams = MIN_CARB_GRAMS
                )
            }
            ?: profileCarbGrams?.takeIf { it > 0 }
            ?: defaultCarbGrams
    }

    fun resolveFatGrams(
        activeCustomPlanCalories: Int?,
        profileFatGrams: Int?,
        defaultFatGrams: Int
    ): Int {
        return activeCustomPlanCalories
            ?.takeIf { it > 0 }
            ?.let { calories ->
                gramsFromCalories(
                    calories = calories,
                    calorieRatio = FAT_CALORIE_RATIO,
                    caloriesPerGram = CALORIES_PER_FAT_GRAM,
                    minimumGrams = MIN_FAT_GRAMS
                )
            }
            ?: profileFatGrams?.takeIf { it > 0 }
            ?: defaultFatGrams
    }

    private fun gramsFromCalories(
        calories: Int,
        calorieRatio: Double,
        caloriesPerGram: Double,
        minimumGrams: Int
    ): Int {
        return ((calories * calorieRatio) / caloriesPerGram)
            .roundToInt()
            .coerceAtLeast(minimumGrams)
    }

    private const val PROTEIN_CALORIE_RATIO = 0.25
    private const val CARB_CALORIE_RATIO = 0.45
    private const val FAT_CALORIE_RATIO = 0.30

    private const val CALORIES_PER_PROTEIN_GRAM = 4.0
    private const val CALORIES_PER_CARB_GRAM = 4.0
    private const val CALORIES_PER_FAT_GRAM = 9.0

    private const val MIN_PROTEIN_GRAMS = 40
    private const val MIN_CARB_GRAMS = 80
    private const val MIN_FAT_GRAMS = 25
}
