package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import kotlin.math.abs

object DietPlanRecommendationResolver {

    fun resolve(
        profile: UserProfileEntity?,
        plans: List<DietPlanUiModel>,
        fallbackPlans: List<DietPlanUiModel>,
        titleTextForPlan: (DietPlanUiModel) -> String
    ): PersonalizedDietRecommendation? {
        val safePlans = plans.ifEmpty { fallbackPlans }
        if (safePlans.isEmpty()) return null

        val scoredPlans = safePlans.map { plan ->
            plan to calculatePlanScore(
                profile = profile,
                plan = plan
            )
        }

        val bestPlan = scoredPlans
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: safePlans.first()

        val bestScore = scoredPlans
            .firstOrNull { (plan, _) -> plan.id == bestPlan.id }
            ?.second
            ?: FALLBACK_MATCH_PERCENT

        return PersonalizedDietRecommendation(
            planId = bestPlan.id,
            titleText = titleTextForPlan(bestPlan),
            reasonRes = reasonResForPlan(bestPlan.id),
            matchPercent = bestScore.coerceIn(MIN_MATCH_PERCENT, MAX_MATCH_PERCENT),
            chip1Res = chipOneResForPlan(bestPlan.id),
            chip2Res = chipTwoResForGoal(profile?.goalType.orEmpty()),
            chip3Res = chipThreeResForProfile(profile)
        )
    }

    fun calculatePlanScore(
        profile: UserProfileEntity?,
        plan: DietPlanUiModel
    ): Int {
        val goal = profile?.goalType.orEmpty().uppercase()
        val targetCalories = profile?.dailyCalorieTarget
            ?.takeIf { calories -> calories > 0 }
            ?: DEFAULT_TARGET_CALORIES

        var score = 0

        score += goalFitScore(
            goal = goal,
            planId = plan.id
        )
        score += calorieFitScore(
            targetCalories = targetCalories,
            planCalories = plan.kcalPerDay
        )
        score += planSafetyScore(
            profile = profile,
            planId = plan.id
        )
        score += durationFitScore(
            profile = profile,
            durationDays = plan.durationDays
        )

        if (plan.isFavorite) {
            score += FAVORITE_BONUS
        }

        return score.coerceIn(0, 100)
    }

    private fun goalFitScore(
        goal: String,
        planId: String
    ): Int {
        return when (goal) {
            "GAIN_WEIGHT" -> when (planId) {
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 50
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 24
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 20
                DietPlanDetailFragment.PLAN_DASH -> 16
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> 4
                DietPlanDetailFragment.PLAN_KETOGENIC -> 4
                else -> 12
            }

            "LOSE_WEIGHT" -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 38
                DietPlanDetailFragment.PLAN_DASH -> 34
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 32
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> 28
                DietPlanDetailFragment.PLAN_KETOGENIC -> 24
                DietPlanDetailFragment.PLAN_MIND -> 22
                DietPlanDetailFragment.PLAN_ELIMINATION -> 16
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 0
                else -> 16
            }

            "MAINTAIN_WEIGHT", "EAT_HEALTHY" -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 46
                DietPlanDetailFragment.PLAN_DASH -> 38
                DietPlanDetailFragment.PLAN_MIND -> 34
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 30
                DietPlanDetailFragment.PLAN_ELIMINATION -> 16
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> 14
                DietPlanDetailFragment.PLAN_KETOGENIC -> 10
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 4
                else -> 20
            }

            "LEARN_NUTRITION" -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 44
                DietPlanDetailFragment.PLAN_DASH -> 36
                DietPlanDetailFragment.PLAN_MIND -> 32
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 30
                DietPlanDetailFragment.PLAN_ELIMINATION -> 20
                DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> 12
                DietPlanDetailFragment.PLAN_KETOGENIC -> 10
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 8
                else -> 20
            }

            else -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 42
                DietPlanDetailFragment.PLAN_DASH -> 34
                DietPlanDetailFragment.PLAN_MIND -> 30
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 28
                else -> 16
            }
        }
    }

    private fun calorieFitScore(
        targetCalories: Int,
        planCalories: Int
    ): Int {
        val difference = abs(targetCalories - planCalories)

        return when {
            difference <= 100 -> 25
            difference <= 200 -> 22
            difference <= 300 -> 18
            difference <= 450 -> 12
            difference <= 650 -> 7
            else -> 2
        }
    }

    private fun planSafetyScore(
        profile: UserProfileEntity?,
        planId: String
    ): Int {
        val planStatus = profile?.planStatus.orEmpty().uppercase()
        val weeklyChange = profile?.weeklyWeightChangeKg ?: 0.0

        var score = 12

        if (planStatus == "AGGRESSIVE" || planStatus == "NOT_RECOMMENDED") {
            score += when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                DietPlanDetailFragment.PLAN_DASH,
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 10

                DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
                DietPlanDetailFragment.PLAN_KETOGENIC -> -8

                else -> 0
            }
        }

        if (weeklyChange < -0.7) {
            score += when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                DietPlanDetailFragment.PLAN_DASH -> 6

                DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
                DietPlanDetailFragment.PLAN_KETOGENIC -> -6

                else -> 0
            }
        }

        if (weeklyChange > 0.2 && planId == DietPlanDetailFragment.PLAN_WEIGHT_GAIN) {
            score += 8
        }

        return score.coerceIn(0, 22)
    }

    private fun durationFitScore(
        profile: UserProfileEntity?,
        durationDays: Int
    ): Int {
        val estimatedWeeks = profile?.estimatedWeeks

        return when {
            estimatedWeeks == null -> 6
            estimatedWeeks <= 4 && durationDays <= 14 -> 8
            estimatedWeeks > 4 && durationDays >= 14 -> 8
            else -> 5
        }
    }

    private fun reasonResForPlan(planId: String): Int {
        return when (planId) {
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN ->
                R.string.diet_plans_personalized_reason_weight_gain

            DietPlanDetailFragment.PLAN_PLANT_BASED ->
                R.string.diet_plans_personalized_reason_plant_based

            DietPlanDetailFragment.PLAN_ELIMINATION ->
                R.string.diet_plans_personalized_reason_elimination

            DietPlanDetailFragment.PLAN_DASH ->
                R.string.diet_plans_personalized_reason_dash

            DietPlanDetailFragment.PLAN_MIND ->
                R.string.diet_plans_personalized_reason_mind

            DietPlanDetailFragment.PLAN_KETOGENIC ->
                R.string.diet_plans_personalized_reason_keto

            else ->
                R.string.diet_plans_personalized_reason_mediterranean
        }
    }

    private fun chipOneResForPlan(planId: String): Int {
        return when (planId) {
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN ->
                R.string.diet_plan_tag_energy_focused

            DietPlanDetailFragment.PLAN_PLANT_BASED ->
                R.string.diet_plan_tag_plant_focused

            DietPlanDetailFragment.PLAN_ELIMINATION ->
                R.string.diet_plan_tag_sensitivity_tracking

            DietPlanDetailFragment.PLAN_DASH ->
                R.string.dash_detail_benefit_2_title

            DietPlanDetailFragment.PLAN_MIND ->
                R.string.diet_plan_tag_brain_friendly

            DietPlanDetailFragment.PLAN_KETOGENIC ->
                R.string.diet_plan_tag_low_carb

            DietPlanCatalog.PLAN_INTERMITTENT_FASTING ->
                R.string.diet_tag_fasting

            else ->
                R.string.diet_plans_personalized_chip_balanced
        }
    }

    private fun chipTwoResForGoal(goalType: String): Int {
        return when (goalType.uppercase()) {
            "GAIN_WEIGHT" -> R.string.diet_plan_tag_energy_focused
            "LOSE_WEIGHT" -> R.string.diet_plans_personalized_chip_easy_start
            "EAT_HEALTHY", "MAINTAIN_WEIGHT", "LEARN_NUTRITION" ->
                R.string.diet_plans_personalized_chip_balanced

            else -> R.string.diet_plans_personalized_chip_balanced
        }
    }

    private fun chipThreeResForProfile(profile: UserProfileEntity?): Int {
        val planStatus = profile?.planStatus.orEmpty().uppercase()

        return if (planStatus == "AGGRESSIVE" || planStatus == "NOT_RECOMMENDED") {
            R.string.diet_plans_personalized_chip_sustainable
        } else {
            R.string.diet_plans_personalized_chip_easy_start
        }
    }

    private const val DEFAULT_TARGET_CALORIES = 1800
    private const val FAVORITE_BONUS = 4
    private const val FALLBACK_MATCH_PERCENT = 86
    private const val MIN_MATCH_PERCENT = 72
    private const val MAX_MATCH_PERCENT = 96
}
