package com.example.fitto.data.local.repository

import android.content.Context
import android.util.Log
import com.example.fitto.data.local.DateKeyFormatter
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.entity.MealEntryEntity
import com.example.fitto.data.local.entity.MealEntryFoodEntity
import com.example.fitto.data.local.model.DailyMealSummary
import com.example.fitto.data.local.model.DailyNutritionSummary
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class MealRepository(context: Context) {

    private val appContext = context.applicationContext
    private val database = FittoDatabaseProvider.getDatabase(appContext)
    private val mealDao = database.mealDao()
    private val foodDao = database.foodDao()
    private val dietPlanDao = database.dietPlanDao()

    private fun currentUserId(): String {
        return UserSessionDataGuard.getActiveUid(appContext) ?: LOCAL_ANONYMOUS_USER_ID
    }

    suspend fun addManualMealEntry(
        mealType: MealType,
        selectedFoods: List<SelectedFoodUiModel>,
        entryDate: String = DateKeyFormatter.today()
    ) {
        if (selectedFoods.isEmpty()) return

        val now = System.currentTimeMillis()
        val mealEntryId = UUID.randomUUID().toString()
        val foodRows = selectedFoods.map { selectedFood ->
            selectedFood.toMealEntryFoodEntity(
                mealEntryId = mealEntryId,
                createdAt = now
            )
        }

        val entry = MealEntryEntity(
            id = mealEntryId,
            userId = currentUserId(),
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
        )

        mealDao.insertMealEntryWithFoods(entry, foodRows)
    }

    suspend fun addPlannedMealEntry(
        mealType: MealType,
        title: String,
        targetCalories: Int,
        plannedMealCalories: Int,
        suggestedFoodNames: List<String> = emptyList(),
        planId: String,
        plannedMealId: String,
        entryDate: String = DateKeyFormatter.today()
    ) {
        if (mealDao.countEntriesForPlannedMeal(currentUserId(), entryDate, plannedMealId) > 0) return

        val now = System.currentTimeMillis()
        val mealEntryId = UUID.randomUUID().toString()

        val allFoods = foodDao.getAllFoods(currentUserId())
        val foodLookup = buildFoodLookup(allFoods)

        val plannedFoods = dietPlanDao.getFoodsForPlannedMeal(plannedMealId)

        val rowsFromPlannedFoods = plannedFoods.mapNotNull { plannedFood ->
            val seedFood = foodDao.getFoodById(currentUserId(), plannedFood.foodId)
                ?: resolveFoodFromLookup(
                    rawKey = plannedFood.foodId,
                    allFoods = allFoods,
                    lookup = foodLookup
                )
                ?: return@mapNotNull null

            val amount = plannedFood.scaledAmountGrams(targetCalories).coerceAtLeast(1.0)

            seedFood.toPlannedMealEntryFoodEntity(
                mealEntryId = mealEntryId,
                amountGrams = amount,
                createdAt = now
            )
        }

        val rowsFromSuggestedNames: List<MealEntryFoodEntity> = if (rowsFromPlannedFoods.isEmpty()) {
            buildRowsFromSuggestedFoodNames(
                mealEntryId = mealEntryId,
                suggestedFoodNames = suggestedFoodNames,
                plannedMealCalories = plannedMealCalories,
                allFoods = allFoods,
                lookup = foodLookup,
                createdAt = now
            )
        } else {
            emptyList()
        }

        val foodRows = when {
            rowsFromPlannedFoods.isNotEmpty() -> rowsFromPlannedFoods
            rowsFromSuggestedNames.isNotEmpty() -> rowsFromSuggestedNames
            else -> listOf(
                createEstimatedPlannedFoodRow(
                    mealEntryId = mealEntryId,
                    title = title,
                    calories = plannedMealCalories,
                    createdAt = now
                )
            )
        }

        val entry = MealEntryEntity(
            id = mealEntryId,
            userId = currentUserId(),
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
            createdAt = now,
            updatedAt = now
        )

        mealDao.insertMealEntryWithFoods(entry, foodRows)
    }

    private fun buildFoodLookup(foods: List<FoodEntity>): Map<String, FoodEntity> {
        val map = linkedMapOf<String, FoodEntity>()

        foods.forEach { food ->
            map[normalizeFoodKey(food.id)] = food
            map[normalizeFoodKey(food.nameTr)] = food

            food.nameEn?.takeIf { it.isNotBlank() }?.let { nameEn ->
                map[normalizeFoodKey(nameEn)] = food
            }
        }

        return map
    }
    private fun resolveFoodFromLookup(
        rawKey: String,
        allFoods: List<FoodEntity>,
        lookup: Map<String, FoodEntity>
    ): FoodEntity? {
        val normalized = normalizeFoodKey(rawKey)

        lookup[normalized]?.let { return it }

        val withoutLastSlash = rawKey.substringAfterLast("/")
        lookup[normalizeFoodKey(withoutLastSlash)]?.let { return it }

        val withoutDash = rawKey.replace("-", " ")
        lookup[normalizeFoodKey(withoutDash)]?.let { return it }

        val withoutUnderscore = rawKey.replace("_", " ")
        lookup[normalizeFoodKey(withoutUnderscore)]?.let { return it }

        if (normalized.length >= 4) {
            allFoods.firstOrNull { food ->
                val foodNameTr = normalizeFoodKey(food.nameTr)
                val foodNameEn = normalizeFoodKey(food.nameEn.orEmpty())

                foodNameTr.contains(normalized) ||
                        normalized.contains(foodNameTr) ||
                        foodNameEn.contains(normalized) ||
                        normalized.contains(foodNameEn)
            }?.let { return it }
        }

        return null
    }
    private fun buildRowsFromSuggestedFoodNames(
        mealEntryId: String,
        suggestedFoodNames: List<String>,
        plannedMealCalories: Int,
        allFoods: List<FoodEntity>,
        lookup: Map<String, FoodEntity>,
        createdAt: Long
    ): List<MealEntryFoodEntity> {
        val matchedFoods = suggestedFoodNames
            .mapNotNull { name ->
                resolveFoodFromLookup(
                    rawKey = name,
                    allFoods = allFoods,
                    lookup = lookup
                )
            }
            .distinctBy { it.id }

        if (matchedFoods.isEmpty()) return emptyList()

        val baseRows = matchedFoods.map { food ->
            val amount = food.defaultServingAmount.takeIf { it > 0.0 } ?: 100.0

            food.toPlannedMealEntryFoodEntity(
                mealEntryId = mealEntryId,
                amountGrams = amount,
                createdAt = createdAt
            )
        }

        val baseCalories = baseRows.sumOf { it.calories }.coerceAtLeast(1)
        val scale = plannedMealCalories.toDouble() / baseCalories.toDouble()

        return matchedFoods.map { food ->
            val baseAmount = food.defaultServingAmount.takeIf { it > 0.0 } ?: 100.0
            val scaledAmount = (baseAmount * scale).coerceIn(1.0, 5000.0)

            food.toPlannedMealEntryFoodEntity(
                mealEntryId = mealEntryId,
                amountGrams = scaledAmount,
                createdAt = createdAt
            )
        }
    }
    private fun createEstimatedPlannedFoodRow(
        mealEntryId: String,
        title: String,
        calories: Int,
        createdAt: Long
    ): MealEntryFoodEntity {
        val safeCalories = calories.coerceAtLeast(0)

        val estimatedProtein = (safeCalories * 0.20 / 4.0).roundOneDecimal()
        val estimatedCarbs = (safeCalories * 0.50 / 4.0).roundOneDecimal()
        val estimatedFat = (safeCalories * 0.30 / 9.0).roundOneDecimal()

        return MealEntryFoodEntity(
            id = UUID.randomUUID().toString(),
            mealEntryId = mealEntryId,
            foodId = null,
            foodName = title,
            amountLabel = "Planlı öğün",
            amountGrams = 0.0,
            calories = safeCalories,
            protein = estimatedProtein,
            carbs = estimatedCarbs,
            fat = estimatedFat,
            createdAt = createdAt
        )
    }
    private fun resolvePlannedFood(
        lookup: Map<String, FoodEntity>,
        rawKey: String
    ): FoodEntity? {
        val normalizedDirect = normalizeFoodKey(rawKey)
        lookup[normalizedDirect]?.let { return it }

        val noSlash = rawKey.substringAfterLast("/")
        lookup[normalizeFoodKey(noSlash)]?.let { return it }

        val noDash = rawKey.replace("-", " ")
        lookup[normalizeFoodKey(noDash)]?.let { return it }

        val noUnderscore = rawKey.replace("_", " ")
        lookup[normalizeFoodKey(noUnderscore)]?.let { return it }

        return null
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

    private fun FoodEntity.toPlannedMealEntryFoodEntity(
        mealEntryId: String,
        amountGrams: Double,
        createdAt: Long
    ): MealEntryFoodEntity {
        val safeAmount = amountGrams.coerceAtLeast(1.0)

        return MealEntryFoodEntity(
            id = UUID.randomUUID().toString(),
            mealEntryId = mealEntryId,
            foodId = id,
            foodName = nameTr,
            amountLabel = formatAmountLabel(safeAmount, defaultUnit.ifBlank { "g" }),
            amountGrams = safeAmount,
            calories = ((caloriesPer100g * safeAmount) / 100.0).roundToInt().coerceAtLeast(0),
            protein = ((proteinPer100g * safeAmount) / 100.0).roundOneDecimal(),
            carbs = ((carbsPer100g * safeAmount) / 100.0).roundOneDecimal(),
            fat = ((fatPer100g * safeAmount) / 100.0).roundOneDecimal(),
            createdAt = createdAt
        )
    }

    private fun formatAmountLabel(amount: Double, unit: String): String {
        val amountText = if (amount % 1.0 == 0.0) {
            amount.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.1f", amount)
        }

        return "$amountText $unit"
    }

    private fun String.humanizeFoodName(): String {
        return this
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale("tr")) else ch.toString()
                }
            }
    }

    suspend fun getAllFoods(): List<FoodEntity> {
        return foodDao.getAllFoods(currentUserId())
    }
    suspend fun getFoodsForMealEntry(entryId: String): List<MealEntryFoodUiModel> {
        return mealDao.getMealEntryFoodsByMealEntryId(currentUserId(), entryId).map { food ->
            MealEntryFoodUiModel(
                id = food.id,
                mealEntryId = food.mealEntryId,
                foodId = food.foodId,
                foodName = food.foodName,
                amountLabel = food.amountLabel,
                amountGrams = food.amountGrams,
                calories = food.calories,
                protein = food.protein,
                carbs = food.carbs,
                fat = food.fat
            )
        }
    }

    suspend fun updateMealEntryFoodAmount(
        foodRowId: String,
        newAmount: Double
    ) {
        val foodRow = mealDao.getMealEntryFoodById(currentUserId(), foodRowId) ?: return
        val entry = mealDao.getMealEntryById(currentUserId(), foodRow.mealEntryId) ?: return

        val safeAmount = newAmount.coerceIn(1.0, 5000.0)
        val oldAmount = foodRow.amountGrams.takeIf { it > 0.0 } ?: return
        val multiplier = safeAmount / oldAmount

        val updatedFood = foodRow.copy(
            amountGrams = safeAmount.roundOneDecimal(),
            amountLabel = safeAmount.formatAmount(foodRow.amountLabel.extractUnitOrDefault()),
            calories = (foodRow.calories * multiplier).roundToInt().coerceAtLeast(0),
            protein = (foodRow.protein * multiplier).roundOneDecimal(),
            carbs = (foodRow.carbs * multiplier).roundOneDecimal(),
            fat = (foodRow.fat * multiplier).roundOneDecimal()
        )

        mealDao.updateMealEntryFood(updatedFood)
        recalculateMealEntryTotals(entry.id)
    }

    suspend fun deleteMealEntryFood(
        foodRowId: String
    ) {
        val foodRow = mealDao.getMealEntryFoodById(currentUserId(), foodRowId) ?: return
        val entryId = foodRow.mealEntryId

        mealDao.deleteMealEntryFoodById(currentUserId(), foodRowId)

        val remainingFoods = mealDao.getMealEntryFoodsByMealEntryId(currentUserId(), entryId)
        if (remainingFoods.isEmpty()) {
            mealDao.deleteMealEntryById(currentUserId(), entryId)
        } else {
            recalculateMealEntryTotals(entryId)
        }
    }

    private suspend fun recalculateMealEntryTotals(entryId: String) {
        val entry = mealDao.getMealEntryById(currentUserId(), entryId) ?: return
        val foods = mealDao.getMealEntryFoodsByMealEntryId(currentUserId(), entryId)

        if (foods.isEmpty()) {
            mealDao.deleteMealEntryById(currentUserId(), entryId)
            return
        }

        val updatedEntry = entry.copy(
            title = foods.toMealTitleFromRows(),
            totalCalories = foods.sumOf { it.calories },
            totalProtein = foods.sumOf { it.protein }.roundOneDecimal(),
            totalCarbs = foods.sumOf { it.carbs }.roundOneDecimal(),
            totalFat = foods.sumOf { it.fat }.roundOneDecimal(),
            updatedAt = System.currentTimeMillis()
        )

        mealDao.updateMealEntry(updatedEntry)
    }

    suspend fun getMealSummariesForToday(): List<DailyMealSummary> {
        return mealDao.getMealSummariesForDate(currentUserId(), DateKeyFormatter.today())
    }
    suspend fun getMealSummariesForDate(dateIso: String): List<DailyMealSummary> {
        return mealDao.getMealSummariesForDate(currentUserId(), dateIso)
    }

    suspend fun getNutritionSummaryForDate(dateIso: String): DailyNutritionSummary {
        return mealDao.getNutritionSummaryForDate(currentUserId(), dateIso)
    }
    suspend fun getNutritionSummaryForToday(): DailyNutritionSummary {
        return mealDao.getNutritionSummaryForDate(currentUserId(), DateKeyFormatter.today())
    }

    suspend fun getMealSummaryForTodayAndType(mealType: MealType): DailyMealSummary {
        return mealDao.getMealSummaryForDateAndMealType(
            userId = currentUserId(),
            date = DateKeyFormatter.today(),
            mealType = mealType.argumentValue
        ) ?: DailyMealSummary(
            mealType = mealType.argumentValue,
            totalCalories = 0,
            totalProtein = 0.0,
            totalCarbs = 0.0,
            totalFat = 0.0
        )
    }

    suspend fun getMealEntryDetailsForToday(mealType: MealType): List<MealEntryDetailUiModel> {
        return getMealEntryDetailsForDate(
            dateIso = DateKeyFormatter.today(),
            mealType = mealType
        )
    }

    suspend fun getMealEntryDetailsForDate(
        dateIso: String,
        mealType: MealType
    ): List<MealEntryDetailUiModel> {
        val rows = mealDao.getMealEntryFoodDetailsForDateAndMealType(
            userId = currentUserId(),
            date = dateIso,
            mealType = mealType.argumentValue
        )

        return rows
            .groupBy { it.mealEntryId }
            .map { (_, foods) ->
                val first = foods.first()
                MealEntryDetailUiModel(
                    entryId = first.mealEntryId,
                    title = first.entryTitle,
                    sourceType = first.sourceType,
                    foodSummary = foods.joinToString(separator = ", ") { food ->
                        "${food.foodName} (${food.amountLabel})"
                    },
                    totalCalories = foods.sumOf { it.calories },
                    totalProtein = foods.sumOf { it.protein }.roundOneDecimal(),
                    totalCarbs = foods.sumOf { it.carbs }.roundOneDecimal(),
                    totalFat = foods.sumOf { it.fat }.roundOneDecimal(),
                    createdAt = foods.minOf { it.createdAt },
                    foodCount = foods.size
                )
            }
            .sortedByDescending { it.createdAt }
    }

    suspend fun deleteMealEntry(entryId: String) {
        mealDao.deleteMealEntryById(currentUserId(), entryId)
    }

    suspend fun scaleMealEntry(entryId: String, multiplier: Double) {
        val safeMultiplier = multiplier.coerceIn(0.5, 2.0)
        if (safeMultiplier == 1.0) return

        val entry = mealDao.getMealEntryById(currentUserId(), entryId) ?: return
        val foods = mealDao.getMealEntryFoodsByEntryId(currentUserId(), entryId)
        if (foods.isEmpty()) return

        val updatedFoods = foods.map { food ->
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

        val updatedEntry = entry.copy(
            title = updatedFoods.toMealTitleFromRows(),
            totalCalories = updatedFoods.sumOf { it.calories },
            totalProtein = updatedFoods.sumOf { it.protein }.roundOneDecimal(),
            totalCarbs = updatedFoods.sumOf { it.carbs }.roundOneDecimal(),
            totalFat = updatedFoods.sumOf { it.fat }.roundOneDecimal(),
            updatedAt = System.currentTimeMillis()
        )

        mealDao.updateMealEntryWithFoods(updatedEntry, updatedFoods)
    }


    private fun com.example.fitto.data.local.entity.PlannedMealFoodEntity.scaledAmountGrams(
        targetCalories: Int
    ): Double {
        if (!scalable) return amountGrams

        val safeTargetCalories = targetCalories.coerceIn(
            com.example.fitto.feature_dietplans.planning.PlannedMealUiModel.MIN_TARGET_CALORIES,
            com.example.fitto.feature_dietplans.planning.PlannedMealUiModel.MAX_TARGET_CALORIES
        )
        val scale = safeTargetCalories.toDouble() /
                com.example.fitto.feature_dietplans.planning.PlannedMealUiModel.BASELINE_CALORIES.toDouble()

        return (amountGrams * scale).coerceIn(minAmountGrams, maxAmountGrams)
    }

    private fun Double.formatAmount(unit: String): String {
        val rounded = roundToInt()
        val safeUnit = if (unit.isBlank()) "g" else unit
        return "$rounded $safeUnit"
    }

    private fun SelectedFoodUiModel.toMealEntryFoodEntity(
        mealEntryId: String,
        createdAt: Long
    ): MealEntryFoodEntity {
        return MealEntryFoodEntity(
            id = UUID.randomUUID().toString(),
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
        val names = take(2).joinToString(separator = ", ") { it.name }
        return if (size > 2) "$names +${size - 2}" else names
    }

    private fun List<MealEntryFoodEntity>.toMealTitleFromRows(): String {
        val names = take(2).joinToString(separator = ", ") { it.foodName }
        return if (size > 2) "$names +${size - 2}" else names
    }

    private fun String.extractUnitOrDefault(): String {
        val unit = trim().substringAfterLast(' ', missingDelimiterValue = "g").trim()
        return unit.takeIf { it.isNotBlank() && it.any { char -> char.isLetter() } } ?: "g"
    }


    private fun Double.roundOneDecimal(): Double = (this * 10.0).roundToInt() / 10.0

    companion object {
        private const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
    }

}