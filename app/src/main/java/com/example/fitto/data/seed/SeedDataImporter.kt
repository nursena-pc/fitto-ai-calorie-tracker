package com.example.fitto.data.seed

import android.content.Context
import android.util.Log
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.entity.DietPlanEntity
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.entity.PlannedMealEntity
import com.example.fitto.data.local.entity.PlannedMealFoodEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object SeedDataImporter {

    private const val TAG = "SeedDataImporter"
    private const val PREFS_NAME = "fitto_seed_prefs"

    private const val KEY_IMPORTED_FOOD_SEED_VERSION = "imported_food_seed_version"
    private const val KEY_IMPORTED_DIET_SEED_VERSION = "imported_diet_seed_version"

    /**
     * v2 = USDA FoodData Central ağırlıklı seed_foods.json.
     * Bu değeri artırınca eski import yapılmış olsa bile foods tablosu güncellenir.
     */
    private const val FOOD_SEED_VERSION = 2

    /**
     * v1 = Hazır diyet planları + planlı öğün + planlı öğün besinleri.
     */
    private const val DIET_SEED_VERSION = 1

    private const val FOOD_SEED_PATH = "seed/seed_foods.json"
    private const val DIET_PLANS_SEED_PATH = "seed/seed_diet_plans.json"
    private const val PLANNED_MEALS_SEED_PATH = "seed/seed_planned_meals.json"
    private const val PLANNED_MEAL_FOODS_SEED_PATH = "seed/seed_planned_meal_foods.json"

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun importIfNeeded(context: Context) {
        appScope.launch {
            runCatching {
                importFoodSeedIfNeeded(context.applicationContext)
                importDietPlanSeedIfNeeded(context.applicationContext)
            }.onFailure { throwable ->
                Log.e(TAG, "Seed import failed", throwable)
            }
        }
    }

    suspend fun importFoodSeedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val importedVersion = prefs.getInt(KEY_IMPORTED_FOOD_SEED_VERSION, 0)

        val database = FittoDatabaseProvider.getDatabase(context)
        val currentFoodCount = database.foodDao().getFoodCount()

        if (importedVersion >= FOOD_SEED_VERSION && currentFoodCount > 0) {
            Log.d(TAG, "Food seed already imported. version=$importedVersion count=$currentFoodCount")
            return
        }

        val foods = readFoodSeed(context)
        database.foodDao().upsertFoods(foods)

        prefs.edit()
            .putInt(KEY_IMPORTED_FOOD_SEED_VERSION, FOOD_SEED_VERSION)
            .apply()

        Log.d(TAG, "Food seed imported. version=$FOOD_SEED_VERSION count=${foods.size}")
    }

    suspend fun importDietPlanSeedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val importedVersion = prefs.getInt(KEY_IMPORTED_DIET_SEED_VERSION, 0)

        val database = FittoDatabaseProvider.getDatabase(context)
        val currentPlanCount = database.dietPlanDao().getDietPlanCount()
        val currentMealCount = database.dietPlanDao().getPlannedMealCount()
        val currentMealFoodCount = database.dietPlanDao().getPlannedMealFoodCount()

        if (
            importedVersion >= DIET_SEED_VERSION &&
            currentPlanCount > 0 &&
            currentMealCount > 0 &&
            currentMealFoodCount > 0
        ) {
            Log.d(
                TAG,
                "Diet seed already imported. version=$importedVersion plans=$currentPlanCount meals=$currentMealCount foods=$currentMealFoodCount"
            )
            return
        }

        val dietPlans = readDietPlanSeed(context)
        val plannedMeals = readPlannedMealSeed(context)
        val plannedMealFoods = readPlannedMealFoodSeed(context)

        database.dietPlanDao().replaceDietSeed(
            plans = dietPlans,
            meals = plannedMeals,
            mealFoods = plannedMealFoods
        )

        prefs.edit()
            .putInt(KEY_IMPORTED_DIET_SEED_VERSION, DIET_SEED_VERSION)
            .apply()

        Log.d(
            TAG,
            "Diet seed imported. version=$DIET_SEED_VERSION plans=${dietPlans.size} meals=${plannedMeals.size} foods=${plannedMealFoods.size}"
        )
    }

    private fun readFoodSeed(context: Context): List<FoodEntity> {
        val array = readArrayFromAsset(
            context = context,
            path = FOOD_SEED_PATH,
            rootArrayName = "foods"
        )

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(item.toFoodEntity())
            }
        }
    }

    private fun readDietPlanSeed(context: Context): List<DietPlanEntity> {
        val array = readArrayFromAsset(
            context = context,
            path = DIET_PLANS_SEED_PATH,
            rootArrayName = "plans"
        )

        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toDietPlanEntity())
            }
        }
    }

    private fun readPlannedMealSeed(context: Context): List<PlannedMealEntity> {
        val array = readArrayFromAsset(
            context = context,
            path = PLANNED_MEALS_SEED_PATH,
            rootArrayName = "plannedMeals"
        )

        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toPlannedMealEntity())
            }
        }
    }

    private fun readPlannedMealFoodSeed(context: Context): List<PlannedMealFoodEntity> {
        val array = readArrayFromAsset(
            context = context,
            path = PLANNED_MEAL_FOODS_SEED_PATH,
            rootArrayName = "plannedMealFoods"
        )

        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toPlannedMealFoodEntity())
            }
        }
    }

    private fun readArrayFromAsset(
        context: Context,
        path: String,
        rootArrayName: String
    ): JSONArray {
        val json = context.assets.open(path)
            .bufferedReader()
            .use { it.readText() }

        return when (json.trim().firstOrNull()) {
            '[' -> JSONArray(json)
            '{' -> JSONObject(json).getJSONArray(rootArrayName)
            else -> JSONArray()
        }
    }

    private fun JSONObject.toFoodEntity(): FoodEntity {
        val now = System.currentTimeMillis()
        return FoodEntity(
            id = getString("id"),
            nameTr = getString("nameTr"),
            nameEn = optStringOrNull("nameEn"),
            category = optString("category", "unknown"),
            defaultUnit = optString("defaultUnit", "g"),
            defaultServingAmount = optDouble("defaultServingAmount", 100.0),
            caloriesPer100g = optDouble("caloriesPer100g", 0.0),
            proteinPer100g = optDouble("proteinPer100g", 0.0),
            carbsPer100g = optDouble("carbsPer100g", 0.0),
            fatPer100g = optDouble("fatPer100g", 0.0),
            fiberPer100g = optNullableDouble("fiberPer100g"),
            source = optString("source", "fitto_seed"),
            isVerified = optBoolean("isVerified", false),
            allergens = optStringArray("allergens").joinToString("|"),
            dietTags = optStringArray("dietTags").joinToString("|"),
            mealTypes = optStringArray("mealTypes").joinToString("|"),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun JSONObject.toDietPlanEntity(): DietPlanEntity {
        val now = System.currentTimeMillis()
        return DietPlanEntity(
            id = getString("id"),
            nameTr = getString("nameTr"),
            nameEn = optStringOrNull("nameEn"),
            durationDays = optInt("durationDays", 7),
            baseCalories = optInt("baseCalories", 1800),
            icon = optString("icon", "🥗"),
            category = optString("category", "balanced"),
            descriptionTr = optString("descriptionTr", ""),
            medicalNoticeTr = optString("medicalNoticeTr", ""),
            isRestrictive = optBoolean("isRestrictive", false),
            source = optString("source", "fitto_seed"),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun JSONObject.toPlannedMealEntity(): PlannedMealEntity {
        val now = System.currentTimeMillis()
        return PlannedMealEntity(
            id = getString("id"),
            dietPlanId = getString("dietPlanId"),
            dayNumber = optInt("dayNumber", 1),
            mealType = optString("mealType", "breakfast"),
            titleTr = getString("titleTr"),
            baseCalories = optInt("baseCalories", 0),
            orderIndex = optInt("orderIndex", 0),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun JSONObject.toPlannedMealFoodEntity(): PlannedMealFoodEntity {
        val now = System.currentTimeMillis()
        val amountGrams = optDouble("amountGrams", 0.0)
        return PlannedMealFoodEntity(
            id = getString("id"),
            plannedMealId = getString("plannedMealId"),
            foodId = getString("foodId"),
            amountGrams = amountGrams,
            displayAmountTr = optString("displayAmountTr", "$amountGrams g"),
            scalable = optBoolean("scalable", true),
            scaleGroup = optString("scaleGroup", "stable"),
            minAmountGrams = optDouble("minAmountGrams", amountGrams),
            maxAmountGrams = optDouble("maxAmountGrams", amountGrams),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optNullableDouble(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optDouble(name)
    }

    private fun JSONObject.optStringArray(name: String): List<String> {
        if (!has(name) || isNull(name)) return emptyList()
        val array = optJSONArray(name) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }
}
