package com.example.fitto.feature_dietplans

import com.example.fitto.R

data class CustomDietPlanDetailLabel(
    val stringRes: Int? = null,
    val fallback: String
)

data class CustomDietPlanMealOrderLabel(
    val order: Int,
    val label: CustomDietPlanDetailLabel
)

object CustomDietPlanDetailLabelResolver {

    fun preference(preference: String): CustomDietPlanDetailLabel {
        return when (preference) {
            "vegetarian" -> stringLabel(R.string.custom_diet_detail_preference_vegetarian, preference)
            "vegan" -> stringLabel(R.string.custom_diet_detail_preference_vegan, preference)
            "gluten_free" -> stringLabel(R.string.custom_diet_detail_preference_gluten_free, preference)
            "lactose_free" -> stringLabel(R.string.custom_diet_detail_preference_lactose_free, preference)
            "high_protein" -> stringLabel(R.string.custom_diet_detail_preference_high_protein, preference)
            "low_carb" -> stringLabel(R.string.custom_diet_detail_preference_low_carb, preference)
            else -> rawLabel(preference)
        }
    }

    fun meal(meal: String): CustomDietPlanDetailLabel {
        return when (meal) {
            "breakfast" -> stringLabel(R.string.create_post_breakfast, meal)
            "snack" -> stringLabel(R.string.create_post_snack, meal)
            "lunch" -> stringLabel(R.string.create_post_lunch, meal)
            "dinner" -> stringLabel(R.string.create_post_dinner, meal)
            else -> rawLabel(meal)
        }
    }

    fun mealOrder(mealOrder: List<String>): List<CustomDietPlanMealOrderLabel> {
        return mealOrder.mapIndexed { index, meal ->
            CustomDietPlanMealOrderLabel(
                order = index + 1,
                label = meal(meal)
            )
        }
    }

    private fun stringLabel(stringRes: Int, fallback: String): CustomDietPlanDetailLabel {
        return CustomDietPlanDetailLabel(
            stringRes = stringRes,
            fallback = fallback
        )
    }

    private fun rawLabel(value: String): CustomDietPlanDetailLabel {
        return CustomDietPlanDetailLabel(fallback = value)
    }
}
