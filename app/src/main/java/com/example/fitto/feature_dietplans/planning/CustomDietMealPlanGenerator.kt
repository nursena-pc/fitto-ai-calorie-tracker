package com.example.fitto.feature_dietplans.planning

import android.content.Context
import com.example.fitto.R
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import java.util.Locale

class CustomDietMealPlanGenerator(
    private val context: Context
) {

    fun createActivePlan(plan: CustomDietPlanLocalModel): ActiveDietPlanUiModel {
        return ActiveDietPlanUiModel(
            id = plan.id,
            title = plan.title,
            badge = context.getString(R.string.custom_diet_plan_active_badge),
            icon = "✨",
            primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
            primaryValue = context.getString(R.string.diet_plans_kcal_per_day, plan.dailyCalories),
            secondaryLabel = context.getString(R.string.create_diet_plan_meal_count_label),
            secondaryValue = plan.mealCount.toString(),
            progress = 0,
            kcalPerDay = plan.dailyCalories,
            durationDays = plan.durationDays
        )
    }

    fun buildDailyPlan(
        plan: CustomDietPlanLocalModel,
        allFoods: List<FoodEntity>,
        dayNumber: Int,
        completedMealIds: Set<String>
    ): DailyPlannedMealsUiModel {
        val safeDuration = CustomDietPlanScheduleResolver.safeDuration(plan.durationDays)
        val normalizedDay = CustomDietPlanScheduleResolver.normalizedDay(
            dayNumber = dayNumber,
            durationDays = plan.durationDays
        )
        val meals = buildMealsForDay(
            plan = plan,
            allFoods = allFoods,
            dayNumber = normalizedDay
        )
        val nextMeal = PlannedMealProgressResolver.nextMeal(
            meals = meals,
            completedMealIds = completedMealIds
        )

        return DailyPlannedMealsUiModel(
            planId = plan.id,
            planTitle = plan.title,
            planBadge = context.getString(R.string.custom_diet_plan_active_badge),
            planIcon = "✨",
            dayNumber = normalizedDay,
            durationDays = safeDuration,
            targetCalories = plan.dailyCalories,
            meals = meals,
            nextMeal = nextMeal,
            completedMealIds = completedMealIds
        )
    }

    private fun buildMealsForDay(
        plan: CustomDietPlanLocalModel,
        allFoods: List<FoodEntity>,
        dayNumber: Int
    ): List<PlannedMealUiModel> {
        val availableFoods = allFoods
            .filter { food -> food.caloriesPer100g > 0.0 }
            .filter { food -> food.isAllowedByPlan(plan) }

        val fallbackFoods = allFoods
            .filter { food -> food.caloriesPer100g > 0.0 }
            .filterNot { food -> food.isExcludedBy(plan.exclusions) }

        val sourceFoods = availableFoods.ifEmpty { fallbackFoods }
        val mealTypes = CustomDietPlanScheduleResolver.mealTypesFor(
            mealOrder = plan.mealOrder,
            mealCount = plan.mealCount
        )
        val ratios = CustomDietPlanScheduleResolver.mealRatios(mealTypes)

        return mealTypes.mapIndexed { index, mealType ->
            val mealFoods = selectFoodsForMeal(
                plan = plan,
                mealType = mealType,
                allFoods = sourceFoods,
                dayNumber = dayNumber,
                mealIndex = index
            )
            val targetMealCalories = CustomDietPlanScheduleResolver.targetMealCalories(
                dailyCalories = plan.dailyCalories,
                ratio = ratios.getValue(mealType)
            )
            val baseCalories = CustomDietPlanScheduleResolver.baseCalories(
                targetMealCalories = targetMealCalories,
                dailyCalories = plan.dailyCalories
            )

            PlannedMealUiModel(
                id = "${plan.id}-day$dayNumber-${mealType.argumentValue}",
                planId = plan.id,
                dayNumber = dayNumber,
                mealType = mealType,
                title = context.getString(
                    R.string.custom_diet_plan_generated_meal_title,
                    context.getString(mealType.titleRes)
                ),
                baseCalories = baseCalories,
                foods = mealFoods.map { food -> food.displayName() }
            )
        }
    }

    private fun selectFoodsForMeal(
        plan: CustomDietPlanLocalModel,
        mealType: MealType,
        allFoods: List<FoodEntity>,
        dayNumber: Int,
        mealIndex: Int
    ): List<FoodEntity> {
        val smartMealFoods = allFoods.filter { food ->
            food.isSmartCandidateForMeal(mealType)
        }

        val mealFoods = smartMealFoods.ifEmpty {
            allFoods.filter { food -> food.supportsMealType(mealType) }
        }

        val candidateFoods = mealFoods.ifEmpty { allFoods }

        val offset = ((dayNumber - 1) * 5) + mealIndex

        fun List<FoodEntity>.rotatedPick(extraOffset: Int = 0): FoodEntity? {
            if (isEmpty()) return null
            val sortedFoods = sortedWith(planComparator(plan, mealType))
            return sortedFoods[(offset + extraOffset).floorMod(sortedFoods.size)]
        }

        val selected = linkedMapOf<String, FoodEntity>()

        fun add(food: FoodEntity?) {
            if (food != null) selected[food.id] = food
        }

        when (mealType) {
            MealType.BREAKFAST -> {
                add(candidateFoods.filter { it.isBreakfastMainCarb() }.rotatedPick(0))
                add(candidateFoods.filter { it.isBreakfastProtein() }.rotatedPick(1))
                add(candidateFoods.filter { it.isBreakfastFruit() }.rotatedPick(2))
                add(candidateFoods.filter { it.isBreakfastProduceOrHealthyFat() }.rotatedPick(3))
            }

            MealType.SNACK -> {
                add(candidateFoods.filter { it.isSnackFruitOrDairy() }.rotatedPick(0))
                add(candidateFoods.filter { it.isHealthyFat() }.rotatedPick(1))

                if (plan.hasPreference(PREF_HIGH_PROTEIN) || plan.goalType == GOAL_MUSCLE || plan.goalType == GOAL_WEIGHT_GAIN) {
                    add(candidateFoods.filter { it.isLightProteinSnack() }.rotatedPick(2))
                }
            }

            MealType.LUNCH,
            MealType.DINNER -> {
                add(candidateFoods.filter { it.isLunchDinnerProtein() }.rotatedPick(0))
                add(candidateFoods.filter { it.isLunchDinnerCarb() }.rotatedPick(1))
                add(candidateFoods.filter { it.isLunchDinnerVegetable() }.rotatedPick(2))
                add(candidateFoods.filter { it.isHealthyFat() }.rotatedPick(3))
            }
        }

        if (selected.isEmpty()) {
            candidateFoods
                .sortedWith(planComparator(plan, mealType))
                .take(DEFAULT_FOOD_COUNT_PER_MEAL)
                .forEach(::add)
        }

        return selected.values.take(
            if (mealType == MealType.SNACK) SNACK_FOOD_COUNT else DEFAULT_FOOD_COUNT_PER_MEAL
        )
    }

    private fun planComparator(
        plan: CustomDietPlanLocalModel,
        mealType: MealType
    ): Comparator<FoodEntity> {
        return compareByDescending<FoodEntity> { food -> food.scoreFor(plan, mealType) }
            .thenBy { food -> food.nameTr }
    }

    private fun FoodEntity.scoreFor(
        plan: CustomDietPlanLocalModel,
        mealType: MealType
    ): Double {
        var score = 0.0
        if (supportsMealType(mealType)) score += 12.0
        if (matchesGoalTag(plan)) score += 10.0
        if (isVerified) score += 2.0

        if (plan.hasPreference(PREF_HIGH_PROTEIN) || plan.goalType == GOAL_MUSCLE) {
            score += proteinPer100g * 0.9
        }

        if (plan.hasPreference(PREF_LOW_CARB) || plan.goalType == GOAL_WEIGHT_LOSS) {
            score -= carbsPer100g * 0.25
            score += proteinPer100g * 0.25
        }

        if (plan.needsEnergyDenseMeals()) {
            score += caloriesPer100g * 0.015
            score += fatPer100g * 0.15
        }

        if (category == CATEGORY_VEGETABLE || category == CATEGORY_FRUIT) {
            score += 2.0
        }

        return score
    }

    private fun FoodEntity.matchesGoalTag(plan: CustomDietPlanLocalModel): Boolean {
        val tag = when (plan.goalType) {
            GOAL_MUSCLE, GOAL_WEIGHT_GAIN -> TAG_WEIGHT_GAIN
            GOAL_MAINTAIN -> TAG_BALANCED
            GOAL_BALANCED -> TAG_BALANCED
            else -> TAG_DASH
        }
        return hasDietTag(tag) || hasDietTag(TAG_BALANCED)
    }

    private fun FoodEntity.isAllowedByPlan(plan: CustomDietPlanLocalModel): Boolean {
        if (isExcludedBy(plan.exclusions)) return false

        if (plan.hasPreference(PREF_VEGAN)) {
            if (!hasDietTag(TAG_PLANT_BASED)) return false
            if (category in animalBasedCategories || category == CATEGORY_DAIRY) return false
        }

        if (plan.hasPreference(PREF_VEGETARIAN)) {
            if (category in animalProteinCategories) return false
        }

        if (plan.hasPreference(PREF_GLUTEN_FREE)) {
            if (normalizedSearchText().containsAny(glutenTerms)) return false
        }

        if (plan.hasPreference(PREF_LACTOSE_FREE)) {
            if (category == CATEGORY_DAIRY || normalizedSearchText().containsAny(lactoseTerms)) return false
        }

        if (plan.hasPreference(PREF_LOW_CARB) && !hasDietTag(TAG_KETO) && carbsPer100g > LOW_CARB_MAX_GRAMS) {
            return false
        }

        return true
    }

    private fun FoodEntity.isExcludedBy(exclusions: List<String>): Boolean {
        if (exclusions.isEmpty()) return false
        val searchText = normalizedSearchText()
        return exclusions
            .map { exclusion -> exclusion.normalizeForSearch() }
            .filter { exclusion -> exclusion.length >= 2 }
            .any { exclusion ->
                searchText.contains(exclusion) ||
                        exclusionAliases.getValueOrDefault(exclusion, emptyList()).any { alias ->
                            searchText.contains(alias)
                        }
            }
    }

    private fun FoodEntity.normalizedSearchText(): String {
        return listOf(id, nameTr, nameEn.orEmpty(), category, allergens, dietTags)
            .joinToString(separator = " ")
            .normalizeForSearch()
    }

    private fun FoodEntity.supportsMealType(mealType: MealType): Boolean {
        return mealTypes.split('|')
            .map { it.trim() }
            .any { it.equals(mealType.argumentValue, ignoreCase = true) }
    }

    private fun FoodEntity.hasDietTag(tag: String): Boolean {
        return dietTags.split('|')
            .map { it.trim() }
            .any { it.equals(tag, ignoreCase = true) }
    }

    private fun FoodEntity.isProteinSource(): Boolean {
        return category in proteinCategories || proteinPer100g >= 8.0
    }

    private fun FoodEntity.isBreakfastCarb(): Boolean {
        return category in setOf(CATEGORY_GRAIN, CATEGORY_FRUIT) || carbsPer100g >= 12.0
    }

    private fun FoodEntity.isLunchDinnerCarb(): Boolean {
        return category in setOf(
            CATEGORY_GRAIN,
            CATEGORY_LEGUME,
            CATEGORY_STARCHY_VEGETABLE,
            CATEGORY_TURKISH_DISH,
            CATEGORY_SOUP
        ) || carbsPer100g >= 15.0
    }

    private fun FoodEntity.isHealthyFat(): Boolean {
        return category in fatCategories || fatPer100g >= 10.0
    }

    private fun FoodEntity.isFruitOrVegetable(): Boolean {
        return category == CATEGORY_FRUIT || category == CATEGORY_VEGETABLE
    }

    private fun FoodEntity.isSnackCore(): Boolean {
        return supportsMealType(MealType.SNACK) &&
                (category in snackCategories || caloriesPer100g in 20.0..700.0)
    }
    private fun FoodEntity.isSmartCandidateForMeal(mealType: MealType): Boolean {
        val text = normalizedSearchText()

        return when (mealType) {
            MealType.BREAKFAST -> {
                if (text.containsAny(breakfastBlockedTerms)) return false

                category in breakfastAllowedCategories ||
                        text.containsAny(breakfastAllowedTerms) ||
                        text.containsAny(breakfastProteinTerms) ||
                        text.containsAny(breakfastProduceTerms)
            }

            MealType.SNACK -> {
                if (text.containsAny(snackBlockedTerms)) return false

                category in snackCategories ||
                        category == CATEGORY_FRUIT ||
                        category == CATEGORY_DAIRY ||
                        category == CATEGORY_NUT ||
                        category == CATEGORY_SEED ||
                        text.containsAny(snackAllowedTerms)
            }

            MealType.LUNCH,
            MealType.DINNER -> {
                if (text.containsAny(lunchDinnerBlockedTerms)) return false

                category in lunchDinnerAllowedCategories ||
                        supportsMealType(mealType)
            }
        }
    }

    private fun FoodEntity.isBreakfastMainCarb(): Boolean {
        val text = normalizedSearchText()

        if (text.containsAny(breakfastBlockedTerms)) return false

        return category == CATEGORY_GRAIN ||
                text.containsAny(breakfastCarbTerms)
    }

    private fun FoodEntity.isBreakfastProtein(): Boolean {
        val text = normalizedSearchText()

        if (text.containsAny(breakfastBlockedTerms)) return false

        return text.containsAny(breakfastProteinTerms) ||
                category == CATEGORY_DAIRY ||
                category == CATEGORY_PLANT_PROTEIN
    }

    private fun FoodEntity.isBreakfastFruit(): Boolean {
        val text = normalizedSearchText()

        if (text.containsAny(breakfastBlockedTerms)) return false

        return category == CATEGORY_FRUIT ||
                text.containsAny(breakfastFruitTerms)
    }

    private fun FoodEntity.isBreakfastProduceOrHealthyFat(): Boolean {
        val text = normalizedSearchText()

        if (text.containsAny(breakfastBlockedTerms)) return false

        return text.containsAny(breakfastProduceTerms) ||
                category == CATEGORY_FAT ||
                category == CATEGORY_NUT ||
                category == CATEGORY_SEED ||
                category == CATEGORY_FRUIT_FAT
    }

    private fun FoodEntity.isSnackFruitOrDairy(): Boolean {
        val text = normalizedSearchText()

        if (text.containsAny(snackBlockedTerms)) return false

        return category == CATEGORY_FRUIT ||
                category == CATEGORY_DAIRY ||
                text.containsAny(snackAllowedTerms)
    }

    private fun FoodEntity.isLightProteinSnack(): Boolean {
        val text = normalizedSearchText()

        if (text.containsAny(snackBlockedTerms)) return false

        return category == CATEGORY_DAIRY ||
                category == CATEGORY_PLANT_PROTEIN ||
                text.containsAny(lightProteinSnackTerms)
    }

    private fun FoodEntity.isLunchDinnerProtein(): Boolean {
        val text = normalizedSearchText()

        return category == CATEGORY_PROTEIN ||
                category == CATEGORY_PLANT_PROTEIN ||
                category == CATEGORY_LEGUME ||
                text.containsAny(lunchDinnerProteinTerms) ||
                proteinPer100g >= 10.0
    }

    private fun FoodEntity.isLunchDinnerVegetable(): Boolean {
        return category == CATEGORY_VEGETABLE ||
                category == CATEGORY_STARCHY_VEGETABLE ||
                category == CATEGORY_SOUP ||
                category == CATEGORY_TURKISH_DISH
    }
    private fun FoodEntity.displayName(): String {
        val localeLanguage = context.resources.configuration.locales[0].language
        return if (localeLanguage.equals(Locale.ENGLISH.language, ignoreCase = true)) {
            nameEn.takeIf { !it.isNullOrBlank() } ?: nameTr
        } else {
            nameTr
        }
    }

    private fun CustomDietPlanLocalModel.hasPreference(preference: String): Boolean {
        return preferences.any { it.equals(preference, ignoreCase = true) }
    }

    private fun CustomDietPlanLocalModel.needsEnergyDenseMeals(): Boolean {
        return goalType == GOAL_MUSCLE || goalType == GOAL_WEIGHT_GAIN || dailyCalories >= 2200
    }

    private fun String.normalizeForSearch(): String {
        return trim()
            .lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun String.containsAny(values: Set<String>): Boolean {
        return values.any { contains(it) }
    }

    private fun Int.floorMod(other: Int): Int {
        return ((this % other) + other) % other
    }

    private fun <K, V> Map<K, V>.getValueOrDefault(key: K, defaultValue: V): V {
        return this[key] ?: defaultValue
    }

    companion object {
        const val PREF_VEGETARIAN = "vegetarian"
        const val PREF_VEGAN = "vegan"
        const val PREF_GLUTEN_FREE = "gluten_free"
        const val PREF_LACTOSE_FREE = "lactose_free"
        const val PREF_HIGH_PROTEIN = "high_protein"
        const val PREF_LOW_CARB = "low_carb"

        private const val GOAL_WEIGHT_LOSS = "WEIGHT_LOSS"
        private const val GOAL_MUSCLE = "MUSCLE"
        private const val GOAL_WEIGHT_GAIN = "WEIGHT_GAIN"
        private const val GOAL_MAINTAIN = "MAINTAIN"
        private const val GOAL_BALANCED = "BALANCED"

        private const val TAG_BALANCED = "balanced"
        private const val TAG_DASH = "dash"
        private const val TAG_KETO = "keto"
        private const val TAG_PLANT_BASED = "plant_based"
        private const val TAG_WEIGHT_GAIN = "weight_gain"

        private const val CATEGORY_BEVERAGE = "beverage"
        private const val CATEGORY_DAIRY = "dairy"
        private const val CATEGORY_DRINK = "drink"
        private const val CATEGORY_FAT = "fat"
        private const val CATEGORY_FRUIT = "fruit"
        private const val CATEGORY_FRUIT_FAT = "fruit_fat"
        private const val CATEGORY_GRAIN = "grain"
        private const val CATEGORY_LEGUME = "legume"
        private const val CATEGORY_NUT = "nut"
        private const val CATEGORY_PLANT_PROTEIN = "plant_protein"
        private const val CATEGORY_PROTEIN = "protein"
        private const val CATEGORY_SEED = "seed"
        private const val CATEGORY_SOUP = "soup"
        private const val CATEGORY_STARCHY_VEGETABLE = "starchy_vegetable"
        private const val CATEGORY_TURKISH_DISH = "turkish_dish"
        private const val CATEGORY_VEGETABLE = "vegetable"

        private const val LOW_CARB_MAX_GRAMS = 12.0
        private const val DEFAULT_FOOD_COUNT_PER_MEAL = 4
        private const val SNACK_FOOD_COUNT = 3

        private val proteinCategories = setOf(
            CATEGORY_PROTEIN,
            CATEGORY_DAIRY,
            CATEGORY_LEGUME,
            CATEGORY_PLANT_PROTEIN
        )

        private val animalProteinCategories = setOf(CATEGORY_PROTEIN)
        private val animalBasedCategories = setOf(CATEGORY_PROTEIN)

        private val fatCategories = setOf(
            CATEGORY_FAT,
            CATEGORY_NUT,
            CATEGORY_SEED,
            CATEGORY_FRUIT_FAT
        )

        private val snackCategories = setOf(
            CATEGORY_FRUIT,
            CATEGORY_NUT,
            CATEGORY_SEED,
            CATEGORY_DAIRY,
            CATEGORY_BEVERAGE,
            CATEGORY_DRINK
        )
        private val breakfastAllowedCategories = setOf(
            CATEGORY_GRAIN,
            CATEGORY_FRUIT,
            CATEGORY_DAIRY,
            CATEGORY_NUT,
            CATEGORY_SEED,
            CATEGORY_FAT,
            CATEGORY_FRUIT_FAT,
            CATEGORY_BEVERAGE,
            CATEGORY_DRINK,
            CATEGORY_PLANT_PROTEIN
        )

        private val lunchDinnerAllowedCategories = setOf(
            CATEGORY_PROTEIN,
            CATEGORY_PLANT_PROTEIN,
            CATEGORY_LEGUME,
            CATEGORY_GRAIN,
            CATEGORY_VEGETABLE,
            CATEGORY_STARCHY_VEGETABLE,
            CATEGORY_TURKISH_DISH,
            CATEGORY_SOUP,
            CATEGORY_FAT
        )

        private val breakfastBlockedTerms = setOf(
            "ispanak",
            "spinach",
            "brokoli",
            "broccoli",
            "lahana",
            "cabbage",
            "karnabahar",
            "cauliflower",
            "kabak",
            "zucchini",
            "patlican",
            "eggplant",
            "corba",
            "soup",
            "tavuk",
            "chicken",
            "somon",
            "salmon",
            "ton baligi",
            "tuna",
            "kofte",
            "meatball",
            "makarna",
            "pasta",
            "pilav",
            "rice",
            "bulgur"
        )

        private val breakfastAllowedTerms = setOf(
            "yumurta",
            "egg",
            "peynir",
            "cheese",
            "yogurt",
            "yoghurt",
            "yogurt",
            "kefir",
            "sut",
            "milk",
            "yulaf",
            "oat",
            "oatmeal",
            "ekmek",
            "bread",
            "tost",
            "toast",
            "granola",
            "cereal",
            "muz",
            "banana",
            "elma",
            "apple",
            "cilek",
            "strawberry",
            "domates",
            "tomato",
            "salatalik",
            "cucumber",
            "zeytin",
            "olive",
            "avokado",
            "avocado"
        )

        private val breakfastCarbTerms = setOf(
            "yulaf",
            "oat",
            "oatmeal",
            "ekmek",
            "bread",
            "tost",
            "toast",
            "granola",
            "cereal",
            "muesli",
            "musli"
        )

        private val breakfastProteinTerms = setOf(
            "yumurta",
            "egg",
            "peynir",
            "cheese",
            "lor",
            "cottage",
            "yogurt",
            "yoghurt",
            "kefir",
            "sut",
            "milk",
            "tofu"
        )

        private val breakfastFruitTerms = setOf(
            "muz",
            "banana",
            "elma",
            "apple",
            "cilek",
            "strawberry",
            "yaban mersini",
            "blueberry",
            "armut",
            "pear",
            "portakal",
            "orange",
            "kivi",
            "kiwi"
        )

        private val breakfastProduceTerms = setOf(
            "domates",
            "tomato",
            "salatalik",
            "cucumber",
            "zeytin",
            "olive",
            "avokado",
            "avocado",
            "biber",
            "pepper"
        )

        private val snackBlockedTerms = setOf(
            "corba",
            "soup",
            "pilav",
            "rice",
            "makarna",
            "pasta",
            "kofte",
            "meatball",
            "tavuk",
            "chicken",
            "balik",
            "fish",
            "somon",
            "salmon"
        )

        private val snackAllowedTerms = setOf(
            "meyve",
            "fruit",
            "elma",
            "apple",
            "muz",
            "banana",
            "yogurt",
            "yoghurt",
            "kefir",
            "kuruyemis",
            "nuts",
            "badem",
            "almond",
            "ceviz",
            "walnut",
            "findik",
            "hazelnut",
            "chia",
            "smoothie"
        )

        private val lightProteinSnackTerms = setOf(
            "yogurt",
            "yoghurt",
            "kefir",
            "sut",
            "milk",
            "tofu",
            "lor",
            "cottage",
            "protein"
        )

        private val lunchDinnerBlockedTerms = setOf(
            "granola",
            "cereal",
            "muesli",
            "musli"
        )

        private val lunchDinnerProteinTerms = setOf(
            "tavuk",
            "chicken",
            "hindi",
            "turkey",
            "balik",
            "fish",
            "somon",
            "salmon",
            "ton baligi",
            "tuna",
            "et",
            "beef",
            "kofte",
            "meatball",
            "mercimek",
            "lentil",
            "nohut",
            "chickpea",
            "fasulye",
            "bean",
            "tofu"
        )
        private val glutenTerms = setOf(
            "bugday",
            "bread",
            "ekmek",
            "pasta",
            "makarna",
            "bulgur",
            "oat",
            "yulaf",
            "grain",
            "tahil",
            "granola"
        )

        private val lactoseTerms = setOf(
            "sut",
            "milk",
            "yogurt",
            "yogurt",
            "kefir",
            "peynir",
            "cheese",
            "dairy"
        )

        private val exclusionAliases = mapOf(
            "brokoli" to listOf("broccoli"),
            "broccoli" to listOf("brokoli"),
            "mantar" to listOf("mushroom"),
            "mushroom" to listOf("mantar"),
            "sut" to listOf("milk", "dairy"),
            "milk" to listOf("sut", "dairy"),
            "fistik" to listOf("peanut"),
            "peanut" to listOf("fistik"),
            "deniz urunu" to listOf("seafood", "fish", "balik", "somon", "levrek", "tuna"),
            "seafood" to listOf("deniz urunu", "fish", "balik", "somon", "levrek", "tuna")
        )
    }
}
