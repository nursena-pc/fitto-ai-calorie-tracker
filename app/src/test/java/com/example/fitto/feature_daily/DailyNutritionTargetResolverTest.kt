package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyNutritionTargetResolverTest {

    @Test
    fun resolveDailyCalories_prioritizesActiveCustomPlan() {
        val target = DailyNutritionTargetResolver.resolveDailyCalories(
            activeCustomPlanCalories = 1800,
            profileDailyCalories = 2000,
            storedDailyCalories = 2200,
            defaultDailyCalories = 2400
        )

        assertEquals(1800, target)
    }

    @Test
    fun resolveDailyCalories_fallsBackToProfileStoredThenDefault() {
        assertEquals(
            2000,
            DailyNutritionTargetResolver.resolveDailyCalories(
                activeCustomPlanCalories = null,
                profileDailyCalories = 2000,
                storedDailyCalories = 2200,
                defaultDailyCalories = 2400
            )
        )
        assertEquals(
            2200,
            DailyNutritionTargetResolver.resolveDailyCalories(
                activeCustomPlanCalories = null,
                profileDailyCalories = 0,
                storedDailyCalories = 2200,
                defaultDailyCalories = 2400
            )
        )
        assertEquals(
            2400,
            DailyNutritionTargetResolver.resolveDailyCalories(
                activeCustomPlanCalories = -1,
                profileDailyCalories = 0,
                storedDailyCalories = 0,
                defaultDailyCalories = 2400
            )
        )
    }

    @Test
    fun resolveMacros_whenActivePlanExists_deriveTargetsFromCalories() {
        assertEquals(
            125,
            DailyNutritionTargetResolver.resolveProteinGrams(
                activeCustomPlanCalories = 2000,
                profileProteinGrams = 90,
                defaultProteinGrams = 75
            )
        )
        assertEquals(
            225,
            DailyNutritionTargetResolver.resolveCarbGrams(
                activeCustomPlanCalories = 2000,
                profileCarbGrams = 250,
                defaultCarbGrams = 220
            )
        )
        assertEquals(
            60,
            DailyNutritionTargetResolver.resolveFatGrams(
                activeCustomPlanCalories = 1800,
                profileFatGrams = 80,
                defaultFatGrams = 70
            )
        )
    }

    @Test
    fun resolveMacros_withoutActivePlan_useProfileThenDefault() {
        assertEquals(
            95,
            DailyNutritionTargetResolver.resolveProteinGrams(
                activeCustomPlanCalories = null,
                profileProteinGrams = 95,
                defaultProteinGrams = 75
            )
        )
        assertEquals(
            220,
            DailyNutritionTargetResolver.resolveCarbGrams(
                activeCustomPlanCalories = null,
                profileCarbGrams = 0,
                defaultCarbGrams = 220
            )
        )
        assertEquals(
            70,
            DailyNutritionTargetResolver.resolveFatGrams(
                activeCustomPlanCalories = -1,
                profileFatGrams = null,
                defaultFatGrams = 70
            )
        )
    }

    @Test
    fun resolveMacros_whenActivePlanIsVeryLow_clampsToMinimumTargets() {
        assertEquals(
            40,
            DailyNutritionTargetResolver.resolveProteinGrams(
                activeCustomPlanCalories = 300,
                profileProteinGrams = 95,
                defaultProteinGrams = 75
            )
        )
        assertEquals(
            80,
            DailyNutritionTargetResolver.resolveCarbGrams(
                activeCustomPlanCalories = 300,
                profileCarbGrams = 240,
                defaultCarbGrams = 220
            )
        )
        assertEquals(
            25,
            DailyNutritionTargetResolver.resolveFatGrams(
                activeCustomPlanCalories = 300,
                profileFatGrams = 75,
                defaultFatGrams = 70
            )
        )
    }
}
