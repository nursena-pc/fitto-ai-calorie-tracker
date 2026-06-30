package com.example.fitto.feature_dietplans.planning

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType

object CustomDietMealFoodSelectionResolver {

    fun sourceFoods(
        plan: CustomDietPlanLocalModel,
        allFoods: List<FoodEntity>
    ): List<FoodEntity> {
        val foodsWithCalories = allFoods.filter { food -> food.caloriesPer100g > 0.0 }
        val availableFoods = foodsWithCalories.filter { food ->
            CustomDietFoodRules.isAllowedByPlan(
                food = food,
                plan = plan
            )
        }
        val fallbackFoods = foodsWithCalories.filterNot { food ->
            CustomDietFoodRules.isExcludedBy(
                food = food,
                exclusions = plan.exclusions
            )
        }

        return availableFoods.ifEmpty { fallbackFoods }
    }

    fun selectFoodsForMeal(
        plan: CustomDietPlanLocalModel,
        mealType: MealType,
        allFoods: List<FoodEntity>,
        dayNumber: Int,
        mealIndex: Int
    ): List<FoodEntity> {
        val smartMealFoods = allFoods.filter { food ->
            CustomDietFoodRules.isSmartCandidateForMeal(
                food = food,
                mealType = mealType
            )
        }

        val mealFoods = smartMealFoods.ifEmpty {
            allFoods.filter { food ->
                CustomDietFoodRules.supportsMealType(
                    food = food,
                    mealType = mealType
                )
            }
        }

        val candidateFoods = mealFoods.ifEmpty { allFoods }
        val offset = ((dayNumber - 1) * DAY_ROTATION_STEP) + mealIndex
        val selected = linkedMapOf<String, FoodEntity>()

        fun List<FoodEntity>.rotatedPick(extraOffset: Int = 0): FoodEntity? {
            if (isEmpty()) return null
            val sortedFoods = sortedWith(planComparator(plan, mealType))
            return sortedFoods[(offset + extraOffset).floorMod(sortedFoods.size)]
        }

        fun add(food: FoodEntity?) {
            if (food != null) selected[food.id] = food
        }

        when (mealType) {
            MealType.BREAKFAST -> {
                add(candidateFoods.filter(CustomDietFoodRules::isBreakfastMainCarb).rotatedPick(0))
                add(candidateFoods.filter(CustomDietFoodRules::isBreakfastProtein).rotatedPick(1))
                add(candidateFoods.filter(CustomDietFoodRules::isBreakfastFruit).rotatedPick(2))
                add(candidateFoods.filter(CustomDietFoodRules::isBreakfastProduceOrHealthyFat).rotatedPick(3))
            }

            MealType.SNACK -> {
                add(candidateFoods.filter(CustomDietFoodRules::isSnackFruitOrDairy).rotatedPick(0))
                add(candidateFoods.filter(CustomDietFoodRules::isHealthyFat).rotatedPick(1))

                if (isProteinFocusedPlan(plan)) {
                    add(candidateFoods.filter(CustomDietFoodRules::isLightProteinSnack).rotatedPick(2))
                }
            }

            MealType.LUNCH,
            MealType.DINNER -> {
                add(candidateFoods.filter(CustomDietFoodRules::isLunchDinnerProtein).rotatedPick(0))
                add(candidateFoods.filter(CustomDietFoodRules::isLunchDinnerCarb).rotatedPick(1))
                add(candidateFoods.filter(CustomDietFoodRules::isLunchDinnerVegetable).rotatedPick(2))
                add(candidateFoods.filter(CustomDietFoodRules::isHealthyFat).rotatedPick(3))
            }
        }

        val targetFoodCount = maxFoodCount(mealType)
        val shouldBackfill = selected.isEmpty() ||
                (mealType != MealType.SNACK && selected.size < targetFoodCount)

        if (shouldBackfill) {
            candidateFoods
                .sortedWith(planComparator(plan, mealType))
                .filterNot { food -> selected.containsKey(food.id) }
                .take(targetFoodCount - selected.size)
                .forEach(::add)
        }

        return selected.values.take(targetFoodCount)
    }

    fun displayNames(
        foods: List<FoodEntity>,
        preferEnglish: Boolean
    ): List<String> {
        return foods.map { food ->
            CustomDietFoodRules.displayName(
                food = food,
                preferEnglish = preferEnglish
            )
        }
    }

    private fun maxFoodCount(mealType: MealType): Int {
        return if (mealType == MealType.SNACK) {
            SNACK_FOOD_COUNT
        } else {
            DEFAULT_FOOD_COUNT_PER_MEAL
        }
    }

    private fun planComparator(
        plan: CustomDietPlanLocalModel,
        mealType: MealType
    ): Comparator<FoodEntity> {
        return compareByDescending<FoodEntity> { food ->
            CustomDietFoodRules.scoreFor(
                food = food,
                plan = plan,
                mealType = mealType
            )
        }.thenBy { food -> food.nameTr }
    }

    private fun isProteinFocusedPlan(plan: CustomDietPlanLocalModel): Boolean {
        return CustomDietFoodRules.hasPreference(
            plan = plan,
            preference = CustomDietFoodRules.PREF_HIGH_PROTEIN
        ) ||
            plan.goalType == GOAL_MUSCLE ||
            plan.goalType == GOAL_WEIGHT_GAIN
    }

    private fun Int.floorMod(other: Int): Int {
        return ((this % other) + other) % other
    }

    private const val GOAL_MUSCLE = "MUSCLE"
    private const val GOAL_WEIGHT_GAIN = "WEIGHT_GAIN"
    private const val DAY_ROTATION_STEP = 5
    private const val DEFAULT_FOOD_COUNT_PER_MEAL = 4
    private const val SNACK_FOOD_COUNT = 3
}
