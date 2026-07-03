package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileNutritionDietSelectionResolverTest {

    @Test
    fun `selecting standard clears every specialized preference`() {
        val result = ProfileNutritionDietSelectionResolver.toggle(
            current = setOf(
                NutritionDietPreference.VEGAN,
                NutritionDietPreference.HIGH_PROTEIN
            ),
            selected = NutritionDietPreference.STANDARD
        )

        assertEquals(setOf(NutritionDietPreference.STANDARD), result)
    }

    @Test
    fun `selecting specialized preference removes standard`() {
        val result = ProfileNutritionDietSelectionResolver.toggle(
            current = setOf(NutritionDietPreference.STANDARD),
            selected = NutritionDietPreference.VEGETARIAN
        )

        assertEquals(setOf(NutritionDietPreference.VEGETARIAN), result)
    }

    @Test
    fun `multiple specialized preferences can be selected`() {
        val result = ProfileNutritionDietSelectionResolver.toggle(
            current = setOf(NutritionDietPreference.VEGETARIAN),
            selected = NutritionDietPreference.GLUTEN_FREE
        )

        assertEquals(
            setOf(
                NutritionDietPreference.VEGETARIAN,
                NutritionDietPreference.GLUTEN_FREE
            ),
            result
        )
    }

    @Test
    fun `removing final specialized preference restores standard`() {
        val result = ProfileNutritionDietSelectionResolver.toggle(
            current = setOf(NutritionDietPreference.VEGAN),
            selected = NutritionDietPreference.VEGAN
        )

        assertEquals(setOf(NutritionDietPreference.STANDARD), result)
    }
}
