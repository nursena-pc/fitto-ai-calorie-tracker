package com.example.fitto.feature_dietplans.data

import android.content.Context
import com.example.fitto.R
import com.example.fitto.feature_dietplans.DietPlanDetailFragment
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import com.example.fitto.feature_dietplans.model.FastingPlanType

object DietPlanCatalog {
    const val PLAN_INTERMITTENT_FASTING = "intermittent_fasting"

    fun getReadyPlans(
        favoriteIds: Set<String> = emptySet(),
        activePlanId: String? = null
    ): List<DietPlanUiModel> {
        return basePlans().map { plan ->
            plan.copy(
                isFavorite = favoriteIds.contains(plan.id),
                isApplied = activePlanId == plan.id
            )
        }
    }

    fun getActivePlan(
        context: Context,
        planId: String,
        fastingType: FastingPlanType? = null,
        customFastingHours: Int? = null
    ): ActiveDietPlanUiModel {
        val normalizedFastingType = fastingType ?: FastingPlanType.PLAN_16_8
        return when (planId) {
            PLAN_INTERMITTENT_FASTING -> createFastingActivePlan(
                context = context,
                fastingType = normalizedFastingType,
                customFastingHours = customFastingHours
            )

            DietPlanDetailFragment.PLAN_KETOGENIC -> ActiveDietPlanUiModel(
                id = planId,
                title = context.getString(R.string.diet_plan_keto_title),
                badge = context.getString(R.string.diet_plan_tag_low_carb),
                icon = "🥑",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 1700),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_keto),
                progress = 42,
                kcalPerDay = 1700,
                durationDays = 21
            )

            DietPlanDetailFragment.PLAN_DASH -> ActiveDietPlanUiModel(
                id = planId,
                title = context.getString(R.string.diet_plan_dash_title),
                badge = context.getString(R.string.dash_detail_benefit_2_title),
                icon = "💚",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 1700),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_dash),
                progress = 38,
                kcalPerDay = 1700,
                durationDays = 14
            )

            DietPlanDetailFragment.PLAN_MIND -> ActiveDietPlanUiModel(
                id = planId,
                title = context.getString(R.string.diet_plan_mind_title),
                badge = context.getString(R.string.diet_plan_tag_brain_friendly),
                icon = "🧠",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 1700),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_mind),
                progress = 36,
                kcalPerDay = 1700,
                durationDays = 14
            )

            DietPlanDetailFragment.PLAN_ELIMINATION -> ActiveDietPlanUiModel(
                id = planId,
                title = context.getString(R.string.diet_plan_elimination_title),
                badge = context.getString(R.string.diet_plan_tag_sensitivity_tracking),
                icon = "🛡️",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 1600),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_elimination),
                progress = 32,
                kcalPerDay = 1600,
                durationDays = 21
            )

            DietPlanDetailFragment.PLAN_PLANT_BASED -> ActiveDietPlanUiModel(
                id = planId,
                title = context.getString(R.string.diet_plan_plant_based_title),
                badge = context.getString(R.string.diet_plan_tag_plant_focused),
                icon = "🌱",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 1600),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_plant_based),
                progress = 44,
                kcalPerDay = 1600,
                durationDays = 14
            )

            DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> ActiveDietPlanUiModel(
                id = planId,
                title = context.getString(R.string.diet_plan_weight_gain_title),
                badge = context.getString(R.string.diet_plan_tag_energy_focused),
                icon = "💪",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 2200),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_weight_gain),
                progress = 30,
                kcalPerDay = 2200,
                durationDays = 21
            )

            else -> ActiveDietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                title = context.getString(R.string.diet_plan_mediterranean_title),
                badge = context.getString(R.string.diet_tag_popular),
                icon = "🥗",
                primaryLabel = context.getString(R.string.daily_active_plan_calorie_target),
                primaryValue = context.getString(R.string.diet_plans_kcal_per_day, 1800),
                secondaryLabel = context.getString(R.string.daily_active_plan_focus),
                secondaryValue = context.getString(R.string.daily_active_plan_focus_mediterranean),
                progress = 40,
                kcalPerDay = 1800,
                durationDays = 7
            )
        }
    }

    fun createEmptyActivePlan(context: Context): ActiveDietPlanUiModel {
        return ActiveDietPlanUiModel(
            id = "none",
            title = context.getString(R.string.daily_no_active_plan_title),
            badge = context.getString(R.string.daily_no_active_plan_badge),
            icon = "🍽️",
            primaryLabel = context.getString(R.string.daily_no_active_plan_primary_label),
            primaryValue = context.getString(R.string.daily_no_active_plan_primary_value),
            secondaryLabel = context.getString(R.string.daily_no_active_plan_secondary_label),
            secondaryValue = context.getString(R.string.daily_no_active_plan_secondary_value),
            progress = 0,
            kcalPerDay = 0,
            durationDays = 0
        )
    }

    private fun createFastingActivePlan(
        context: Context,
        fastingType: FastingPlanType,
        customFastingHours: Int?
    ): ActiveDietPlanUiModel {
        val fastingSummary = createFastingSummary(context, fastingType, customFastingHours)
        return ActiveDietPlanUiModel(
            id = PLAN_INTERMITTENT_FASTING,
            title = context.getString(R.string.diet_plan_if_title),
            badge = fastingSummary.badge,
            icon = "⏳",
            primaryLabel = context.getString(R.string.daily_fasting_duration),
            primaryValue = context.getString(R.string.daily_fasting_duration_mock),
            secondaryLabel = context.getString(R.string.daily_eating_window),
            secondaryValue = fastingSummary.eatingWindow,
            progress = 69,
            kcalPerDay = 1600,
            durationDays = 14
        )
    }

    private fun createFastingSummary(
        context: Context,
        fastingType: FastingPlanType,
        customFastingHours: Int?
    ): FastingSummary {
        return when (fastingType) {
            FastingPlanType.PLAN_12_12 -> FastingSummary(
                badge = context.getString(R.string.daily_fasting_badge_12_12),
                eatingWindow = "08:00 - 20:00"
            )

            FastingPlanType.PLAN_15_9 -> FastingSummary(
                badge = context.getString(R.string.daily_fasting_badge_15_9),
                eatingWindow = "12:00 - 21:00"
            )

            FastingPlanType.PLAN_16_8 -> FastingSummary(
                badge = context.getString(R.string.daily_active_plan_badge),
                eatingWindow = context.getString(R.string.daily_eating_window_mock)
            )

            FastingPlanType.PLAN_6_1 -> FastingSummary(
                badge = context.getString(R.string.daily_fasting_badge_6_1),
                eatingWindow = context.getString(R.string.daily_fasting_weekly_window)
            )

            FastingPlanType.PLAN_5_2 -> FastingSummary(
                badge = context.getString(R.string.daily_fasting_badge_5_2),
                eatingWindow = context.getString(R.string.daily_fasting_weekly_window)
            )

            FastingPlanType.PLAN_CUSTOM -> {
                val fastingHours = customFastingHours ?: 16
                val eatingHours = 24 - fastingHours
                FastingSummary(
                    badge = context.getString(
                        R.string.daily_fasting_badge_custom,
                        fastingHours,
                        eatingHours
                    ),
                    eatingWindow = context.getString(
                        R.string.daily_fasting_custom_window,
                        eatingHours
                    )
                )
            }
        }
    }

    private fun basePlans(): List<DietPlanUiModel> {
        return listOf(
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                titleRes = R.string.diet_plan_mediterranean_title,
                descriptionRes = R.string.diet_plan_mediterranean_desc,
                kcalPerDay = 1800,
                durationDays = 7,
                isFavorite = false,
                tagRes = R.string.diet_tag_popular,
                imageResId = R.drawable.akdeniz_detay_header
            ),
            DietPlanUiModel(
                id = PLAN_INTERMITTENT_FASTING,
                titleRes = R.string.diet_plan_if_title,
                descriptionRes = R.string.diet_plan_if_desc,
                kcalPerDay = 1600,
                durationDays = 14,
                isFavorite = true,
                tagRes = R.string.diet_tag_fasting,
                imageResId = R.drawable.intermittent_fasting_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_KETOGENIC,
                titleRes = R.string.diet_plan_keto_title,
                descriptionRes = R.string.diet_plan_keto_desc,
                kcalPerDay = 1700,
                durationDays = 21,
                tagRes = R.string.diet_tag_low_carb,
                imageResId = R.drawable.ketogenic_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_DASH,
                titleRes = R.string.diet_plan_dash_title,
                descriptionRes = R.string.diet_plan_dash_desc,
                kcalPerDay = 1700,
                durationDays = 14,
                tagRes = R.string.dash_detail_benefit_2_title,
                imageResId = R.drawable.dash_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_MIND,
                titleRes = R.string.diet_plan_mind_title,
                descriptionRes = R.string.diet_plan_mind_desc,
                kcalPerDay = 1700,
                durationDays = 14,
                tagRes = R.string.diet_plan_tag_brain_friendly,
                imageResId = R.drawable.mind_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_ELIMINATION,
                titleRes = R.string.diet_plan_elimination_title,
                descriptionRes = R.string.diet_plan_elimination_desc,
                kcalPerDay = 1600,
                durationDays = 21,
                tagRes = R.string.diet_plan_tag_sensitivity_tracking,
                imageResId = R.drawable.elimination_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_PLANT_BASED,
                titleRes = R.string.diet_plan_plant_based_title,
                descriptionRes = R.string.diet_plan_plant_based_desc,
                kcalPerDay = 1600,
                durationDays = 14,
                tagRes = R.string.diet_plan_tag_plant_focused,
                imageResId = R.drawable.plant_based_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
                titleRes = R.string.diet_plan_weight_gain_title,
                descriptionRes = R.string.diet_plan_weight_gain_desc,
                kcalPerDay = 2200,
                durationDays = 21,
                tagRes = R.string.diet_plan_tag_energy_focused,
                imageResId = R.drawable.weight_gain_diet_cover
            )
        )
    }

    private data class FastingSummary(
        val badge: String,
        val eatingWindow: String
    )
}
