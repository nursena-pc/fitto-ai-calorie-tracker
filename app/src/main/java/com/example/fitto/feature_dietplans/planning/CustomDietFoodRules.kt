package com.example.fitto.feature_dietplans.planning

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import java.text.Normalizer
import java.util.Locale

object CustomDietFoodRules {
    const val PREF_VEGETARIAN = "vegetarian"
    const val PREF_VEGAN = "vegan"
    const val PREF_GLUTEN_FREE = "gluten_free"
    const val PREF_LACTOSE_FREE = "lactose_free"
    const val PREF_HIGH_PROTEIN = "high_protein"
    const val PREF_LOW_CARB = "low_carb"

    fun scoreFor(
        food: FoodEntity,
        plan: CustomDietPlanLocalModel,
        mealType: MealType
    ): Double {
        var score = 0.0
        if (supportsMealType(food, mealType)) score += 12.0
        if (food.matchesGoalTag(plan)) score += 10.0
        if (food.isVerified) score += 2.0

        if (hasPreference(plan, PREF_HIGH_PROTEIN) || plan.goalType == GOAL_MUSCLE) {
            score += food.proteinPer100g * 0.9
        }

        if (hasPreference(plan, PREF_LOW_CARB) || plan.goalType == GOAL_WEIGHT_LOSS) {
            score -= food.carbsPer100g * 0.25
            score += food.proteinPer100g * 0.25
        }

        if (needsEnergyDenseMeals(plan)) {
            score += food.caloriesPer100g * 0.015
            score += food.fatPer100g * 0.15
        }

        if (food.category == CATEGORY_VEGETABLE || food.category == CATEGORY_FRUIT) {
            score += 2.0
        }

        return score
    }

    fun isAllowedByPlan(
        food: FoodEntity,
        plan: CustomDietPlanLocalModel
    ): Boolean {
        if (isExcludedBy(food, plan.exclusions)) return false

        if (hasPreference(plan, PREF_VEGAN)) {
            if (!hasDietTag(food, TAG_PLANT_BASED)) return false
            if (food.category in animalBasedCategories || food.category == CATEGORY_DAIRY) return false
        }

        if (hasPreference(plan, PREF_VEGETARIAN)) {
            if (food.category in animalProteinCategories) return false
        }

        if (hasPreference(plan, PREF_GLUTEN_FREE)) {
            if (normalizedSearchText(food).containsAny(glutenTerms)) return false
        }

        if (hasPreference(plan, PREF_LACTOSE_FREE)) {
            if (food.category == CATEGORY_DAIRY || normalizedSearchText(food).containsAny(lactoseTerms)) {
                return false
            }
        }

        if (
            hasPreference(plan, PREF_LOW_CARB) &&
            !hasDietTag(food, TAG_KETO) &&
            food.carbsPer100g > LOW_CARB_MAX_GRAMS
        ) {
            return false
        }

        return true
    }

    fun isExcludedBy(
        food: FoodEntity,
        exclusions: List<String>
    ): Boolean {
        if (exclusions.isEmpty()) return false
        val searchText = normalizedSearchText(food)

        return exclusions
            .map { exclusion -> exclusion.toSearchKey() }
            .filter { exclusion -> exclusion.length >= MIN_EXCLUSION_LENGTH }
            .any { exclusion ->
                searchText.contains(exclusion) ||
                    exclusionAliases.getValueOrDefault(exclusion, emptyList()).any { alias ->
                        searchText.contains(alias)
                    }
            }
    }

    fun normalizedSearchText(food: FoodEntity): String {
        return listOf(
            food.id,
            food.nameTr,
            food.nameEn.orEmpty(),
            food.category,
            food.allergens,
            food.dietTags
        ).joinToString(separator = " ")
            .toSearchKey()
    }

    fun displayName(
        food: FoodEntity,
        preferEnglish: Boolean
    ): String {
        return if (preferEnglish) {
            food.nameEn.takeIf { name -> !name.isNullOrBlank() } ?: food.nameTr
        } else {
            food.nameTr
        }
    }

    fun supportsMealType(
        food: FoodEntity,
        mealType: MealType
    ): Boolean {
        return food.mealTypes.split('|')
            .map { value -> value.trim() }
            .any { value -> value.equals(mealType.argumentValue, ignoreCase = true) }
    }

    fun hasDietTag(
        food: FoodEntity,
        tag: String
    ): Boolean {
        return food.dietTags.split('|')
            .map { value -> value.trim() }
            .any { value -> value.equals(tag, ignoreCase = true) }
    }

    fun isSmartCandidateForMeal(
        food: FoodEntity,
        mealType: MealType
    ): Boolean {
        val text = normalizedSearchText(food)

        return when (mealType) {
            MealType.BREAKFAST -> {
                if (text.containsAny(breakfastBlockedTerms)) return false

                food.category in breakfastAllowedCategories ||
                    text.containsAny(breakfastAllowedTerms) ||
                    text.containsAny(breakfastProteinTerms) ||
                    text.containsAny(breakfastProduceTerms)
            }

            MealType.SNACK -> {
                if (text.containsAny(snackBlockedTerms)) return false

                food.category in snackCategories ||
                    food.category == CATEGORY_FRUIT ||
                    food.category == CATEGORY_DAIRY ||
                    food.category == CATEGORY_NUT ||
                    food.category == CATEGORY_SEED ||
                    text.containsAny(snackAllowedTerms)
            }

            MealType.LUNCH,
            MealType.DINNER -> {
                if (text.containsAny(lunchDinnerBlockedTerms)) return false

                food.category in lunchDinnerAllowedCategories ||
                    supportsMealType(food, mealType)
            }
        }
    }

    fun isBreakfastMainCarb(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)
        if (text.containsAny(breakfastBlockedTerms)) return false

        return food.category == CATEGORY_GRAIN ||
            text.containsAny(breakfastCarbTerms)
    }

    fun isBreakfastProtein(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)
        if (text.containsAny(breakfastBlockedTerms)) return false

        return text.containsAny(breakfastProteinTerms) ||
            food.category == CATEGORY_DAIRY ||
            food.category == CATEGORY_PLANT_PROTEIN
    }

    fun isBreakfastFruit(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)
        if (text.containsAny(breakfastBlockedTerms)) return false

        return food.category == CATEGORY_FRUIT ||
            text.containsAny(breakfastFruitTerms)
    }

    fun isBreakfastProduceOrHealthyFat(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)
        if (text.containsAny(breakfastBlockedTerms)) return false

        return text.containsAny(breakfastProduceTerms) ||
            food.category == CATEGORY_FAT ||
            food.category == CATEGORY_NUT ||
            food.category == CATEGORY_SEED ||
            food.category == CATEGORY_FRUIT_FAT
    }

    fun isSnackFruitOrDairy(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)
        if (text.containsAny(snackBlockedTerms)) return false

        return food.category == CATEGORY_FRUIT ||
            food.category == CATEGORY_DAIRY ||
            text.containsAny(snackAllowedTerms)
    }

    fun isLightProteinSnack(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)
        if (text.containsAny(snackBlockedTerms)) return false

        return food.category == CATEGORY_DAIRY ||
            food.category == CATEGORY_PLANT_PROTEIN ||
            text.containsAny(lightProteinSnackTerms)
    }

    fun isLunchDinnerProtein(food: FoodEntity): Boolean {
        val text = normalizedSearchText(food)

        return food.category == CATEGORY_PROTEIN ||
            food.category == CATEGORY_PLANT_PROTEIN ||
            food.category == CATEGORY_LEGUME ||
            text.containsAny(lunchDinnerProteinTerms) ||
            food.proteinPer100g >= LUNCH_DINNER_PROTEIN_THRESHOLD
    }

    fun isLunchDinnerCarb(food: FoodEntity): Boolean {
        return food.category in lunchDinnerCarbCategories ||
            food.carbsPer100g >= LUNCH_DINNER_CARB_THRESHOLD
    }

    fun isLunchDinnerVegetable(food: FoodEntity): Boolean {
        return food.category == CATEGORY_VEGETABLE ||
            food.category == CATEGORY_STARCHY_VEGETABLE ||
            food.category == CATEGORY_SOUP ||
            food.category == CATEGORY_TURKISH_DISH
    }

    fun isHealthyFat(food: FoodEntity): Boolean {
        return food.category in fatCategories || food.fatPer100g >= HEALTHY_FAT_THRESHOLD
    }

    fun hasPreference(
        plan: CustomDietPlanLocalModel,
        preference: String
    ): Boolean {
        return plan.preferences.any { value -> value.equals(preference, ignoreCase = true) }
    }

    fun needsEnergyDenseMeals(plan: CustomDietPlanLocalModel): Boolean {
        return plan.goalType == GOAL_MUSCLE ||
            plan.goalType == GOAL_WEIGHT_GAIN ||
            plan.dailyCalories >= ENERGY_DENSE_CALORIES
    }

    fun normalizeForSearch(value: String): String {
        return value.toSearchKey()
    }

    private fun FoodEntity.matchesGoalTag(plan: CustomDietPlanLocalModel): Boolean {
        val tag = when (plan.goalType) {
            GOAL_MUSCLE, GOAL_WEIGHT_GAIN -> TAG_WEIGHT_GAIN
            GOAL_MAINTAIN -> TAG_BALANCED
            GOAL_BALANCED -> TAG_BALANCED
            else -> TAG_DASH
        }
        return hasDietTag(this, tag) || hasDietTag(this, TAG_BALANCED)
    }

    private fun String.toSearchKey(): String {
        val textWithoutDiacritics = Normalizer.normalize(
            trim().lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        ).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        return textWithoutDiacritics
            .replace("\u0131", "i")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun String.containsAny(values: Set<String>): Boolean {
        return values.any { value -> contains(value) }
    }

    private fun <K, V> Map<K, V>.getValueOrDefault(key: K, defaultValue: V): V {
        return this[key] ?: defaultValue
    }

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
    private const val HEALTHY_FAT_THRESHOLD = 10.0
    private const val LUNCH_DINNER_PROTEIN_THRESHOLD = 10.0
    private const val LUNCH_DINNER_CARB_THRESHOLD = 15.0
    private const val ENERGY_DENSE_CALORIES = 2200
    private const val MIN_EXCLUSION_LENGTH = 2

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

    private val lunchDinnerCarbCategories = setOf(
        CATEGORY_GRAIN,
        CATEGORY_LEGUME,
        CATEGORY_STARCHY_VEGETABLE,
        CATEGORY_TURKISH_DISH,
        CATEGORY_SOUP
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
        "yumurta",
        "egg",
        "tofu",
        "et",
        "meat",
        "kofte",
        "meatball",
        "nohut",
        "chickpea",
        "mercimek",
        "lentil",
        "fasulye",
        "bean"
    )

    private val glutenTerms = setOf(
        "bugday",
        "wheat",
        "ekmek",
        "bread",
        "makarna",
        "pasta",
        "bulgur",
        "arpa",
        "barley",
        "cavdar",
        "rye"
    )

    private val lactoseTerms = setOf(
        "sut",
        "milk",
        "yogurt",
        "yoghurt",
        "peynir",
        "cheese",
        "kefir"
    )

    private val exclusionAliases = mapOf(
        "sut" to listOf("milk", "yogurt", "yoghurt", "peynir", "cheese", "kefir"),
        "milk" to listOf("sut", "yogurt", "yoghurt", "peynir", "cheese", "kefir"),
        "peynir" to listOf("cheese", "sut", "milk"),
        "cheese" to listOf("peynir", "sut", "milk"),
        "mantar" to listOf("mushroom"),
        "mushroom" to listOf("mantar"),
        "deniz urunu" to listOf("seafood", "balik", "fish", "somon", "salmon", "ton baligi", "tuna"),
        "seafood" to listOf("deniz urunu", "balik", "fish", "somon", "salmon", "ton baligi", "tuna"),
        "yer fistigi" to listOf("peanut", "fistik", "peanut butter"),
        "peanut" to listOf("yer fistigi", "fistik", "peanut butter")
    )
}
