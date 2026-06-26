package com.example.fitto.feature_daily

import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_meal.model.MealType

object DailyReminderTargetResolver {

    sealed interface Target {
        data class Meal(val mealType: MealType) : Target
        data object Water : Target
        data object Exercise : Target
        data object DailySummary : Target
    }

    fun resolve(
        categoryName: String?,
        reminderId: String?
    ): Target {
        return when (parseCategory(categoryName)) {
            ReminderCategory.MEAL -> Target.Meal(mealTypeForReminder(reminderId))
            ReminderCategory.WATER -> Target.Water
            ReminderCategory.EXERCISE -> Target.Exercise
            else -> Target.DailySummary
        }
    }

    private fun parseCategory(categoryName: String?): ReminderCategory? {
        return runCatching {
            ReminderCategory.valueOf(categoryName.orEmpty())
        }.getOrNull()
    }

    private fun mealTypeForReminder(reminderId: String?): MealType {
        return when (reminderId) {
            "lunch" -> MealType.LUNCH
            "dinner" -> MealType.DINNER
            "snack" -> MealType.SNACK
            else -> MealType.BREAKFAST
        }
    }
}
