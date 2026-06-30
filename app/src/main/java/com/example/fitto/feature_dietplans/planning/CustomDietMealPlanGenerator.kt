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
        val sourceFoods = CustomDietMealFoodSelectionResolver.sourceFoods(
            plan = plan,
            allFoods = allFoods
        )
        val mealTypes = CustomDietPlanScheduleResolver.mealTypesFor(
            mealOrder = plan.mealOrder,
            mealCount = plan.mealCount
        )
        val ratios = CustomDietPlanScheduleResolver.mealRatios(mealTypes)

        return mealTypes.mapIndexed { index, mealType ->
            val mealFoods = CustomDietMealFoodSelectionResolver.selectFoodsForMeal(
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
                foods = CustomDietMealFoodSelectionResolver.displayNames(
                    foods = mealFoods,
                    preferEnglish = shouldPreferEnglishFoodNames()
                )
            )
        }
    }

    private fun shouldPreferEnglishFoodNames(): Boolean {
        val localeLanguage = context.resources.configuration.locales[0].language
        return localeLanguage.equals(
            Locale.ENGLISH.language,
            ignoreCase = true
        )
    }

    companion object {
        const val PREF_VEGETARIAN = CustomDietFoodRules.PREF_VEGETARIAN
        const val PREF_VEGAN = CustomDietFoodRules.PREF_VEGAN
        const val PREF_GLUTEN_FREE = CustomDietFoodRules.PREF_GLUTEN_FREE
        const val PREF_LACTOSE_FREE = CustomDietFoodRules.PREF_LACTOSE_FREE
        const val PREF_HIGH_PROTEIN = CustomDietFoodRules.PREF_HIGH_PROTEIN
        const val PREF_LOW_CARB = CustomDietFoodRules.PREF_LOW_CARB

    }
}
