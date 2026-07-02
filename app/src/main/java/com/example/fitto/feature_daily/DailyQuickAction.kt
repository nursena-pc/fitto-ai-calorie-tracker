package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import java.util.Locale

enum class DailyQuickAction {
    SEARCH_FOOD,
    CAMERA_FOOD,
    CREATE_FOOD,
    WATER,
    ACTIVITY,
    DIET_PLAN;

    companion object {
        fun fromValue(value: String?): DailyQuickAction? {
            val normalized = value
                ?.trim()
                ?.uppercase(Locale.ROOT)
                ?.takeIf(String::isNotEmpty)
                ?: return null

            return entries.firstOrNull { action -> action.name == normalized }
        }
    }
}

sealed interface DailyQuickActionDestination {
    data class AddMeal(val mealType: MealType) : DailyQuickActionDestination
    data class CameraFood(val mealType: MealType) : DailyQuickActionDestination
    data object AddWater : DailyQuickActionDestination
    data object AddExercise : DailyQuickActionDestination
    data object DietPlans : DailyQuickActionDestination
}

object DailyQuickActionDestinationResolver {
    fun resolve(action: DailyQuickAction): DailyQuickActionDestination {
        return when (action) {
            DailyQuickAction.SEARCH_FOOD,
            DailyQuickAction.CREATE_FOOD -> {
                DailyQuickActionDestination.AddMeal(MealType.BREAKFAST)
            }

            DailyQuickAction.CAMERA_FOOD -> {
                DailyQuickActionDestination.CameraFood(MealType.BREAKFAST)
            }

            DailyQuickAction.WATER -> DailyQuickActionDestination.AddWater
            DailyQuickAction.ACTIVITY -> DailyQuickActionDestination.AddExercise
            DailyQuickAction.DIET_PLAN -> DailyQuickActionDestination.DietPlans
        }
    }
}
