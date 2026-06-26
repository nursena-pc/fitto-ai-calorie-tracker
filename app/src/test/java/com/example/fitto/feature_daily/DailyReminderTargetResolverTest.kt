package com.example.fitto.feature_daily

import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyReminderTargetResolverTest {

    @Test
    fun resolve_mealCategoryMapsReminderIdsToMealTypes() {
        assertEquals(
            DailyReminderTargetResolver.Target.Meal(MealType.BREAKFAST),
            DailyReminderTargetResolver.resolve("MEAL", "breakfast")
        )
        assertEquals(
            DailyReminderTargetResolver.Target.Meal(MealType.LUNCH),
            DailyReminderTargetResolver.resolve("MEAL", "lunch")
        )
        assertEquals(
            DailyReminderTargetResolver.Target.Meal(MealType.DINNER),
            DailyReminderTargetResolver.resolve("MEAL", "dinner")
        )
        assertEquals(
            DailyReminderTargetResolver.Target.Meal(MealType.SNACK),
            DailyReminderTargetResolver.resolve("MEAL", "snack")
        )
    }

    @Test
    fun resolve_nonMealCategoriesMapToCardTargets() {
        assertEquals(
            DailyReminderTargetResolver.Target.Water,
            DailyReminderTargetResolver.resolve("WATER", null)
        )
        assertEquals(
            DailyReminderTargetResolver.Target.Exercise,
            DailyReminderTargetResolver.resolve("EXERCISE", null)
        )
    }

    @Test
    fun resolve_unknownCategoryUsesDailySummary() {
        assertEquals(
            DailyReminderTargetResolver.Target.DailySummary,
            DailyReminderTargetResolver.resolve("CUSTOM", null)
        )
        assertEquals(
            DailyReminderTargetResolver.Target.DailySummary,
            DailyReminderTargetResolver.resolve("unknown", null)
        )
    }
}
