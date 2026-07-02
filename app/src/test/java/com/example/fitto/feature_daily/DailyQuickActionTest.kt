package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class DailyQuickActionTest {

    @Test
    fun `serialized quick action ignores casing and surrounding spaces`() {
        assertEquals(
            DailyQuickAction.CAMERA_FOOD,
            DailyQuickAction.fromValue("  camera_food  ")
        )
    }


    @Test
    fun `serialized action parsing is locale independent`() {
        val originalLocale = Locale.getDefault()

        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))

            assertEquals(
                DailyQuickAction.DIET_PLAN,
                DailyQuickAction.fromValue("diet_plan")
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `unknown serialized quick action is ignored`() {
        assertNull(DailyQuickAction.fromValue("unknown"))
        assertNull(DailyQuickAction.fromValue(null))
    }

    @Test
    fun `search and create actions open breakfast add meal`() {
        assertEquals(
            DailyQuickActionDestination.AddMeal(MealType.BREAKFAST),
            DailyQuickActionDestinationResolver.resolve(DailyQuickAction.SEARCH_FOOD)
        )
        assertEquals(
            DailyQuickActionDestination.AddMeal(MealType.BREAKFAST),
            DailyQuickActionDestinationResolver.resolve(DailyQuickAction.CREATE_FOOD)
        )
    }

    @Test
    fun `camera action opens breakfast camera entry`() {
        assertEquals(
            DailyQuickActionDestination.CameraFood(MealType.BREAKFAST),
            DailyQuickActionDestinationResolver.resolve(DailyQuickAction.CAMERA_FOOD)
        )
    }

    @Test
    fun `non meal quick actions keep their dedicated destinations`() {
        assertEquals(
            DailyQuickActionDestination.AddWater,
            DailyQuickActionDestinationResolver.resolve(DailyQuickAction.WATER)
        )
        assertEquals(
            DailyQuickActionDestination.AddExercise,
            DailyQuickActionDestinationResolver.resolve(DailyQuickAction.ACTIVITY)
        )
        assertEquals(
            DailyQuickActionDestination.DietPlans,
            DailyQuickActionDestinationResolver.resolve(DailyQuickAction.DIET_PLAN)
        )
    }
}
