package com.example.fitto.feature_home.presentation.profile

internal object ProfileNutritionDietSelectionResolver {

    fun toggle(
        current: Set<NutritionDietPreference>,
        selected: NutritionDietPreference
    ): Set<NutritionDietPreference> {
        if (selected == NutritionDietPreference.STANDARD) {
            return setOf(NutritionDietPreference.STANDARD)
        }

        val updated = current
            .minus(NutritionDietPreference.STANDARD)
            .toMutableSet()

        if (!updated.add(selected)) {
            updated.remove(selected)
        }

        return updated.ifEmpty {
            mutableSetOf(NutritionDietPreference.STANDARD)
        }
    }
}
