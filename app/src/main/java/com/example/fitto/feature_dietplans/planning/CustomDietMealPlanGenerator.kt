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
            .filter { food ->
                CustomDietFoodRules.isAllowedByPlan(
                    food = food,
                    plan = plan
                )
            }

        val fallbackFoods = allFoods
            .filter { food -> food.caloriesPer100g > 0.0 }
            .filterNot { food ->
                CustomDietFoodRules.isExcludedBy(
                    food = food,
                    exclusions = plan.exclusions
                )
            }

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
        return compareByDescending<FoodEntity> { food ->
            CustomDietFoodRules.scoreFor(
                food = food,
                plan = plan,
                mealType = mealType
            )
        }
            .thenBy { food -> food.nameTr }
    }

    private fun FoodEntity.displayName(): String {
        val localeLanguage = context.resources.configuration.locales[0].language
        return CustomDietFoodRules.displayName(
            food = this,
            preferEnglish = localeLanguage.equals(Locale.ENGLISH.language, ignoreCase = true)
        )
    }

    private fun Int.floorMod(other: Int): Int {
        return ((this % other) + other) % other
    }

    private fun isProteinFocusedPlan(plan: CustomDietPlanLocalModel): Boolean {
        return CustomDietFoodRules.hasPreference(
            plan = plan,
            preference = CustomDietFoodRules.PREF_HIGH_PROTEIN
        ) ||
            plan.goalType == GOAL_MUSCLE ||
            plan.goalType == GOAL_WEIGHT_GAIN
    }

    companion object {
        const val PREF_VEGETARIAN = CustomDietFoodRules.PREF_VEGETARIAN
        const val PREF_VEGAN = CustomDietFoodRules.PREF_VEGAN
        const val PREF_GLUTEN_FREE = CustomDietFoodRules.PREF_GLUTEN_FREE
        const val PREF_LACTOSE_FREE = CustomDietFoodRules.PREF_LACTOSE_FREE
        const val PREF_HIGH_PROTEIN = CustomDietFoodRules.PREF_HIGH_PROTEIN
        const val PREF_LOW_CARB = CustomDietFoodRules.PREF_LOW_CARB

        private const val GOAL_MUSCLE = "MUSCLE"
        private const val GOAL_WEIGHT_GAIN = "WEIGHT_GAIN"
        private const val DEFAULT_FOOD_COUNT_PER_MEAL = 4
        private const val SNACK_FOOD_COUNT = 3
    }
}
