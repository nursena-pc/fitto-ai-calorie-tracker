package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.DietPlanEntity
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.PlannedMealUiModel
import com.example.fitto.feature_meal.model.MealType

fun dailyTestActivePlan(
    id: String = "balanced",
    title: String = "Balanced plan",
    badge: String = "7 days",
    icon: String = "icon",
    durationDays: Int = 7
): ActiveDietPlanUiModel {
    return ActiveDietPlanUiModel(
        id = id,
        title = title,
        badge = badge,
        icon = icon,
        primaryLabel = "Calories",
        primaryValue = "1800 kcal",
        secondaryLabel = "Focus",
        secondaryValue = "Balanced",
        progress = 30,
        kcalPerDay = 1800,
        durationDays = durationDays
    )
}

fun dailyTestPlannedMeal(
    id: String = "breakfast",
    planId: String = "balanced",
    dayNumber: Int = 2,
    mealType: MealType = MealType.BREAKFAST,
    title: String = "Breakfast",
    baseCalories: Int = 450,
    foods: List<String> = listOf("Egg", "Toast")
): PlannedMealUiModel {
    return PlannedMealUiModel(
        id = id,
        planId = planId,
        dayNumber = dayNumber,
        mealType = mealType,
        title = title,
        baseCalories = baseCalories,
        foods = foods
    )
}

fun dailyTestMealPlan(
    planId: String = "balanced",
    dayNumber: Int = 2,
    durationDays: Int = 7,
    targetCalories: Int = 1800,
    meals: List<PlannedMealUiModel> = listOf(dailyTestPlannedMeal()),
    nextMeal: PlannedMealUiModel? = meals.firstOrNull(),
    completedMealIds: Set<String> = emptySet()
): DailyPlannedMealsUiModel {
    return DailyPlannedMealsUiModel(
        planId = planId,
        planTitle = "Balanced plan",
        planBadge = "7 days",
        planIcon = "icon",
        dayNumber = dayNumber,
        durationDays = durationDays,
        targetCalories = targetCalories,
        meals = meals,
        nextMeal = nextMeal,
        completedMealIds = completedMealIds
    )
}

fun dailyTestCustomPlan(
    id: String = "custom_plan_1",
    durationDays: Int = 14,
    dailyCalories: Int = 1900
): CustomDietPlanLocalModel {
    return CustomDietPlanLocalModel(
        id = id,
        title = "My plan",
        description = "Description",
        goalType = "balanced",
        dailyCalories = dailyCalories,
        mealCount = 4,
        durationDays = durationDays,
        isPublic = false,
        preferences = emptyList(),
        exclusions = emptyList(),
        mealOrder = emptyList(),
        isDraft = false,
        createdAt = 1L,
        updatedAt = 1L
    )
}

fun dailyTestDietPlanEntity(
    id: String = "balanced",
    durationDays: Int = 7,
    nameTr: String = "Oda planı",
    icon: String = "room-icon"
): DietPlanEntity {
    return DietPlanEntity(
        id = id,
        nameTr = nameTr,
        nameEn = "Room plan",
        durationDays = durationDays,
        baseCalories = 1800,
        icon = icon,
        category = "balanced",
        descriptionTr = "Description",
        medicalNoticeTr = "Notice",
        isRestrictive = false,
        source = "seed",
        createdAt = 1L,
        updatedAt = 1L
    )
}

fun dailyTestFood(id: String = "food-1"): FoodEntity {
    return FoodEntity(
        id = id,
        nameTr = "Yumurta",
        nameEn = "Egg",
        category = "protein",
        defaultUnit = "g",
        defaultServingAmount = 100.0,
        caloriesPer100g = 150.0,
        proteinPer100g = 12.0,
        carbsPer100g = 1.0,
        fatPer100g = 10.0,
        fiberPer100g = 0.0,
        source = "seed",
        isVerified = true,
        allergens = "",
        dietTags = "",
        mealTypes = "breakfast",
        createdAt = 1L,
        updatedAt = 1L
    )
}
