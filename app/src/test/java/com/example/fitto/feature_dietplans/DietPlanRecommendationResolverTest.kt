package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DietPlanRecommendationResolverTest {

    @Test
    fun resolve_gainWeightProfileSelectsWeightGainPlan() {
        val result = DietPlanRecommendationResolver.resolve(
            profile = profile(
                goalType = "GAIN_WEIGHT",
                dailyCalorieTarget = 2200,
                weeklyWeightChangeKg = 0.3,
                estimatedWeeks = 12
            ),
            plans = listOf(
                plan(DietPlanDetailFragment.PLAN_MEDITERRANEAN, kcalPerDay = 1800),
                plan(DietPlanDetailFragment.PLAN_WEIGHT_GAIN, kcalPerDay = 2200, durationDays = 21),
                plan(DietPlanDetailFragment.PLAN_DASH, kcalPerDay = 1700)
            ),
            fallbackPlans = emptyList(),
            titleTextForPlan = { plan -> "Title ${plan.id}" }
        )

        assertNotNull(result)
        assertEquals(DietPlanDetailFragment.PLAN_WEIGHT_GAIN, result?.planId)
        assertEquals("Title ${DietPlanDetailFragment.PLAN_WEIGHT_GAIN}", result?.titleText)
        assertEquals(R.string.diet_plans_personalized_reason_weight_gain, result?.reasonRes)
        assertEquals(R.string.diet_plan_tag_energy_focused, result?.chip1Res)
    }

    @Test
    fun resolve_aggressiveWeightLossProfilePrefersBalancedHeartFriendlyPlan() {
        val result = DietPlanRecommendationResolver.resolve(
            profile = profile(
                goalType = "LOSE_WEIGHT",
                dailyCalorieTarget = 1600,
                weeklyWeightChangeKg = -0.8,
                planStatus = "AGGRESSIVE"
            ),
            plans = listOf(
                plan(DietPlanCatalog.PLAN_INTERMITTENT_FASTING, kcalPerDay = 1600),
                plan(DietPlanDetailFragment.PLAN_KETOGENIC, kcalPerDay = 1700),
                plan(DietPlanDetailFragment.PLAN_DASH, kcalPerDay = 1700),
                plan(DietPlanDetailFragment.PLAN_WEIGHT_GAIN, kcalPerDay = 2200)
            ),
            fallbackPlans = emptyList(),
            titleTextForPlan = DietPlanUiModel::id
        )

        assertNotNull(result)
        assertEquals(DietPlanDetailFragment.PLAN_DASH, result?.planId)
        assertEquals(R.string.diet_plans_personalized_reason_dash, result?.reasonRes)
        assertEquals(R.string.diet_plans_personalized_chip_sustainable, result?.chip3Res)
    }

    @Test
    fun resolve_usesFallbackPlansWhenPlanListIsEmpty() {
        val result = DietPlanRecommendationResolver.resolve(
            profile = null,
            plans = emptyList(),
            fallbackPlans = listOf(plan(DietPlanDetailFragment.PLAN_MEDITERRANEAN)),
            titleTextForPlan = { "Fallback title" }
        )

        assertNotNull(result)
        assertEquals(DietPlanDetailFragment.PLAN_MEDITERRANEAN, result?.planId)
        assertEquals("Fallback title", result?.titleText)
    }

    @Test
    fun resolve_clampsDisplayedMatchPercent() {
        val result = DietPlanRecommendationResolver.resolve(
            profile = profile(
                goalType = "GAIN_WEIGHT",
                dailyCalorieTarget = 2200,
                weeklyWeightChangeKg = 0.3,
                estimatedWeeks = 12
            ),
            plans = listOf(
                plan(
                    id = DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
                    kcalPerDay = 2200,
                    durationDays = 21,
                    isFavorite = true
                )
            ),
            fallbackPlans = emptyList(),
            titleTextForPlan = DietPlanUiModel::id
        )

        assertEquals(96, result?.matchPercent)
    }

    @Test
    fun calculatePlanScore_clampsHighScoresAtOneHundred() {
        val score = DietPlanRecommendationResolver.calculatePlanScore(
            profile = profile(
                goalType = "GAIN_WEIGHT",
                dailyCalorieTarget = 2200,
                weeklyWeightChangeKg = 0.3,
                estimatedWeeks = 12
            ),
            plan = plan(
                id = DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
                kcalPerDay = 2200,
                durationDays = 21,
                isFavorite = true
            )
        )

        assertEquals(100, score)
    }

    private fun plan(
        id: String,
        kcalPerDay: Int = 1800,
        durationDays: Int = 14,
        isFavorite: Boolean = false
    ): DietPlanUiModel {
        return DietPlanUiModel(
            id = id,
            titleText = id,
            kcalPerDay = kcalPerDay,
            durationDays = durationDays,
            isFavorite = isFavorite
        )
    }

    private fun profile(
        goalType: String,
        dailyCalorieTarget: Int,
        weeklyWeightChangeKg: Double = 0.0,
        estimatedWeeks: Int? = null,
        planStatus: String = ""
    ): UserProfileEntity {
        return UserProfileEntity(
            userId = "user",
            email = "user@example.com",
            displayName = "User",
            userName = "user",
            heightCm = 170,
            weightKg = 70,
            targetWeightKg = null,
            age = 30,
            gender = "FEMALE",
            goalType = goalType,
            activityLevel = "MODERATE",
            bmr = 1400,
            maintenanceCalories = 2000,
            targetCalories = dailyCalorieTarget,
            recommendedCalories = dailyCalorieTarget,
            dailyCalorieTarget = dailyCalorieTarget,
            proteinTargetGrams = 120,
            carbTargetGrams = 200,
            fatTargetGrams = 60,
            waterTargetMl = 2000,
            weeklyWeightChangeKg = weeklyWeightChangeKg,
            estimatedWeeks = estimatedWeeks,
            planStatus = planStatus,
            profileCompleted = true,
            updatedAt = 0L,
            syncedAt = 0L
        )
    }
}
