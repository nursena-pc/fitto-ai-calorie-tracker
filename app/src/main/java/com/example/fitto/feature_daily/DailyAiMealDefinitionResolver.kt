package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.feature_meal.model.MealType

object DailyAiMealDefinitionResolver {

    data class DefinitionRes(
        val key: String,
        @StringRes val labelRes: Int
    )

    fun resolveMealDefinitions(): List<DefinitionRes> {
        return MealType.entries.map { mealType ->
            DefinitionRes(
                key = mealType.argumentValue,
                labelRes = mealType.titleRes
            )
        }
    }
}
