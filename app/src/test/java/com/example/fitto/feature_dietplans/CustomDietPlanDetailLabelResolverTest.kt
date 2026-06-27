package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomDietPlanDetailLabelResolverTest {

    @Test
    fun `known preference keys map to localized string resources`() {
        assertEquals(
            CustomDietPlanDetailLabel(
                stringRes = R.string.custom_diet_detail_preference_vegetarian,
                fallback = "vegetarian"
            ),
            CustomDietPlanDetailLabelResolver.preference("vegetarian")
        )
        assertEquals(
            CustomDietPlanDetailLabel(
                stringRes = R.string.custom_diet_detail_preference_high_protein,
                fallback = "high_protein"
            ),
            CustomDietPlanDetailLabelResolver.preference("high_protein")
        )
    }

    @Test
    fun `unknown preference key is kept as raw fallback label`() {
        val label = CustomDietPlanDetailLabelResolver.preference("pescatarian")

        assertNull(label.stringRes)
        assertEquals("pescatarian", label.fallback)
    }

    @Test
    fun `known meal keys map to localized string resources`() {
        assertEquals(
            CustomDietPlanDetailLabel(
                stringRes = R.string.create_post_breakfast,
                fallback = "breakfast"
            ),
            CustomDietPlanDetailLabelResolver.meal("breakfast")
        )
        assertEquals(
            CustomDietPlanDetailLabel(
                stringRes = R.string.create_post_dinner,
                fallback = "dinner"
            ),
            CustomDietPlanDetailLabelResolver.meal("dinner")
        )
    }

    @Test
    fun `unknown meal key is kept as raw fallback label`() {
        val label = CustomDietPlanDetailLabelResolver.meal("brunch")

        assertNull(label.stringRes)
        assertEquals("brunch", label.fallback)
    }

    @Test
    fun `meal order keeps one based order and maps labels`() {
        val labels = CustomDietPlanDetailLabelResolver.mealOrder(
            listOf("breakfast", "custom_meal")
        )

        assertEquals(1, labels[0].order)
        assertEquals(R.string.create_post_breakfast, labels[0].label.stringRes)
        assertEquals("breakfast", labels[0].label.fallback)
        assertEquals(2, labels[1].order)
        assertNull(labels[1].label.stringRes)
        assertEquals("custom_meal", labels[1].label.fallback)
    }
}
