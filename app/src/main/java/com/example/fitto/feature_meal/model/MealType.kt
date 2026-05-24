package com.example.fitto.feature_meal.model

import androidx.annotation.StringRes
import com.example.fitto.R

enum class MealType(
    val argumentValue: String,
    @StringRes val titleRes: Int
) {
    BREAKFAST("breakfast", R.string.daily_breakfast),
    SNACK("snack", R.string.daily_snack),
    LUNCH("lunch", R.string.daily_lunch),
    DINNER("dinner", R.string.daily_dinner);

    companion object {
        fun fromArgument(value: String?): MealType {
            return entries.firstOrNull { it.argumentValue == value } ?: BREAKFAST
        }
    }
}
