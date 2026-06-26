package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.feature_dietplans.model.DietPlanDetailUiModel

object DietPlanDetailCatalog {

    val all: Map<String, DietPlanDetailUiModel> = mapOf(
        DietPlanDetailFragment.PLAN_MEDITERRANEAN to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_MEDITERRANEAN,
            titleRes = R.string.diet_plan_detail_title_mediterranean,
            coverImageRes = R.drawable.akdeniz_detay_header,

            badge1Res = R.string.diet_plan_detail_badge_calorie,
            badge2Res = R.string.diet_plan_detail_badge_duration,
            badge3Res = R.string.diet_plan_detail_badge_heart,
            badge4Res = R.string.diet_plan_detail_badge_balanced,

            aboutIcon = "🍃",
            aboutDescRes = R.string.diet_plan_detail_about_desc,

            benefit1Icon = "❤️",
            benefit1TitleRes = R.string.diet_plan_detail_benefit_1_title,
            benefit1DescRes = R.string.diet_plan_detail_benefit_1_desc,

            benefit2Icon = "🥬",
            benefit2TitleRes = R.string.diet_plan_detail_benefit_2_title,
            benefit2DescRes = R.string.diet_plan_detail_benefit_2_desc,

            benefit3Icon = "⚖️",
            benefit3TitleRes = R.string.diet_plan_detail_benefit_3_title,
            benefit3DescRes = R.string.diet_plan_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.diet_plan_detail_meal_breakfast_title,
            sampleMeal1DescRes = R.string.diet_plan_detail_meal_breakfast_desc,
            sampleMeal1KcalRes = R.string.diet_plan_detail_meal_breakfast_kcal,

            sampleMeal2TitleRes = R.string.diet_plan_detail_meal_lunch_title,
            sampleMeal2DescRes = R.string.diet_plan_detail_meal_lunch_desc,
            sampleMeal2KcalRes = R.string.diet_plan_detail_meal_lunch_kcal,

            sampleMeal3TitleRes = R.string.diet_plan_detail_meal_snack_title,
            sampleMeal3DescRes = R.string.diet_plan_detail_meal_snack_desc,
            sampleMeal3KcalRes = R.string.diet_plan_detail_meal_snack_kcal,

            sampleMeal4TitleRes = R.string.diet_plan_detail_meal_dinner_title,
            sampleMeal4DescRes = R.string.diet_plan_detail_meal_dinner_desc,
            sampleMeal4KcalRes = R.string.diet_plan_detail_meal_dinner_kcal,

            featuredFoodCount = 4,

            featuredFood1Icon = "🫒",
            featuredFood1Res = R.string.diet_plan_detail_food_olive_oil,

            featuredFood2Icon = "🥬",
            featuredFood2Res = R.string.diet_plan_detail_food_vegetables,

            featuredFood3Icon = "🐟",
            featuredFood3Res = R.string.diet_plan_detail_food_fish,

            featuredFood4Icon = "🌾",
            featuredFood4Res = R.string.diet_plan_detail_food_whole_grain,

            featuredFood5Icon = "🥜",
            featuredFood5Res = R.string.diet_plan_detail_food_nuts,

            featuredFood6Icon = "🍅",
            featuredFood6Res = R.string.diet_plan_detail_food_vegetables,

            forWhom1Res = R.string.diet_plan_detail_suitable_beginner,
            forWhom2Res = R.string.diet_plan_detail_suitable_sustainable,
            forWhom3Res = R.string.diet_plan_detail_suitable_balance,

            macroTitleRes = R.string.diet_plan_detail_macro_title,
            macroCarbRes = R.string.diet_plan_detail_macro_carb,
            macroProteinRes = R.string.diet_plan_detail_macro_protein,
            macroFatRes = R.string.diet_plan_detail_macro_fat,

            attentionIcon = "⚠️",
            attention1Res = R.string.diet_plan_detail_attention_1,
            attention2Res = R.string.diet_plan_detail_attention_2,
            attention3Res = R.string.diet_plan_detail_attention_3
        ),

        DietPlanDetailFragment.PLAN_KETOGENIC to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_KETOGENIC,
            titleRes = R.string.keto_detail_title,
            coverImageRes = R.drawable.ketogenic_diet_cover,

            badge1Res = R.string.keto_detail_badge_calorie,
            badge2Res = R.string.keto_detail_badge_duration,
            badge3Res = R.string.keto_detail_badge_low_carb,
            badge4Res = R.string.keto_detail_badge_high_fat,

            aboutIcon = "🥑",
            aboutDescRes = R.string.keto_detail_about_desc,

            benefit1Icon = "🔥",
            benefit1TitleRes = R.string.keto_detail_benefit_1_title,
            benefit1DescRes = R.string.keto_detail_benefit_1_desc,

            benefit2Icon = "🥑",
            benefit2TitleRes = R.string.keto_detail_benefit_2_title,
            benefit2DescRes = R.string.keto_detail_benefit_2_desc,

            benefit3Icon = "📉",
            benefit3TitleRes = R.string.keto_detail_benefit_3_title,
            benefit3DescRes = R.string.keto_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.keto_detail_meal_1_title,
            sampleMeal1DescRes = R.string.keto_detail_meal_1_desc,
            sampleMeal1KcalRes = R.string.keto_detail_meal_1_kcal,

            sampleMeal2TitleRes = R.string.keto_detail_meal_2_title,
            sampleMeal2DescRes = R.string.keto_detail_meal_2_desc,
            sampleMeal2KcalRes = R.string.keto_detail_meal_2_kcal,

            sampleMeal3TitleRes = R.string.keto_detail_meal_3_title,
            sampleMeal3DescRes = R.string.keto_detail_meal_3_desc,
            sampleMeal3KcalRes = R.string.keto_detail_meal_3_kcal,

            sampleMeal4TitleRes = R.string.keto_detail_meal_4_title,
            sampleMeal4DescRes = R.string.keto_detail_meal_4_desc,
            sampleMeal4KcalRes = R.string.keto_detail_meal_4_kcal,

            featuredFoodCount = 6,

            featuredFood1Icon = "🥑",
            featuredFood1Res = R.string.keto_detail_food_avocado,

            featuredFood2Icon = "🥚",
            featuredFood2Res = R.string.keto_detail_food_egg,

            featuredFood3Icon = "🐟",
            featuredFood3Res = R.string.keto_detail_food_salmon,

            featuredFood4Icon = "🧀",
            featuredFood4Res = R.string.keto_detail_food_cheese,

            featuredFood5Icon = "🫒",
            featuredFood5Res = R.string.keto_detail_food_olive_oil,

            featuredFood6Icon = "🥜",
            featuredFood6Res = R.string.keto_detail_food_nuts,

            forWhom1Res = R.string.keto_detail_for_whom_1,
            forWhom2Res = R.string.keto_detail_for_whom_2,
            forWhom3Res = R.string.keto_detail_for_whom_3,

            macroTitleRes = R.string.keto_detail_macro_title,
            macroCarbRes = R.string.keto_detail_macro_carb,
            macroProteinRes = R.string.keto_detail_macro_protein,
            macroFatRes = R.string.keto_detail_macro_fat,

            attentionIcon = "⚠️",
            attention1Res = R.string.keto_detail_attention_1,
            attention2Res = R.string.keto_detail_attention_2,
            attention3Res = R.string.keto_detail_attention_3
        ),
        DietPlanDetailFragment.PLAN_DASH to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_DASH,
            titleRes = R.string.dash_detail_title,
            coverImageRes = R.drawable.dash_diet_cover,

            badge1Res = R.string.dash_detail_badge_calorie,
            badge2Res = R.string.dash_detail_badge_duration,
            badge3Res = R.string.dash_detail_badge_pressure,
            badge4Res = R.string.dash_detail_badge_low_sodium,

            aboutIcon = "💚",
            aboutDescRes = R.string.dash_detail_about_desc,

            benefit1Icon = "💚",
            benefit1TitleRes = R.string.dash_detail_benefit_1_title,
            benefit1DescRes = R.string.dash_detail_benefit_1_desc,

            benefit2Icon = "💧",
            benefit2TitleRes = R.string.dash_detail_benefit_2_title,
            benefit2DescRes = R.string.dash_detail_benefit_2_desc,

            benefit3Icon = "⚖️",
            benefit3TitleRes = R.string.dash_detail_benefit_3_title,
            benefit3DescRes = R.string.dash_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.dash_detail_meal_1_title,
            sampleMeal1DescRes = R.string.dash_detail_meal_1_desc,
            sampleMeal1KcalRes = R.string.dash_detail_meal_1_kcal,

            sampleMeal2TitleRes = R.string.dash_detail_meal_2_title,
            sampleMeal2DescRes = R.string.dash_detail_meal_2_desc,
            sampleMeal2KcalRes = R.string.dash_detail_meal_2_kcal,

            sampleMeal3TitleRes = R.string.dash_detail_meal_3_title,
            sampleMeal3DescRes = R.string.dash_detail_meal_3_desc,
            sampleMeal3KcalRes = R.string.dash_detail_meal_3_kcal,

            sampleMeal4TitleRes = R.string.dash_detail_meal_4_title,
            sampleMeal4DescRes = R.string.dash_detail_meal_4_desc,
            sampleMeal4KcalRes = R.string.dash_detail_meal_4_kcal,

            featuredFoodCount = 5,

            featuredFood1Icon = "🥬",
            featuredFood1Res = R.string.dash_detail_food_vegetables,

            featuredFood2Icon = "🍎",
            featuredFood2Res = R.string.dash_detail_food_fruits,

            featuredFood3Icon = "🌾",
            featuredFood3Res = R.string.dash_detail_food_whole_grains,

            featuredFood4Icon = "🥛",
            featuredFood4Res = R.string.dash_detail_food_low_fat_dairy,

            featuredFood5Icon = "🥜",
            featuredFood5Res = R.string.dash_detail_food_nuts,

            featuredFood6Icon = "🐟",
            featuredFood6Res = R.string.dash_detail_food_fish,

            forWhom1Res = R.string.dash_detail_for_whom_1,
            forWhom2Res = R.string.dash_detail_for_whom_2,
            forWhom3Res = R.string.dash_detail_for_whom_3,

            macroTitleRes = R.string.dash_detail_macro_title,
            macroCarbRes = R.string.dash_detail_macro_carb,
            macroProteinRes = R.string.dash_detail_macro_protein,
            macroFatRes = R.string.dash_detail_macro_fat,

            attentionIcon = "🛡️",
            attention1Res = R.string.dash_detail_attention_1,
            attention2Res = R.string.dash_detail_attention_2,
            attention3Res = R.string.dash_detail_attention_3
        ),
        DietPlanDetailFragment.PLAN_MIND to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_MIND,
            titleRes = R.string.mind_detail_title,
            coverImageRes = R.drawable.mind_diet_cover,

            badge1Res = R.string.mind_detail_badge_calorie,
            badge2Res = R.string.mind_detail_badge_duration,
            badge3Res = R.string.mind_detail_badge_brain,
            badge4Res = R.string.mind_detail_badge_focus,

            aboutIcon = "🧠",
            aboutDescRes = R.string.mind_detail_about_desc,

            benefit1Icon = "🧠",
            benefit1TitleRes = R.string.mind_detail_benefit_1_title,
            benefit1DescRes = R.string.mind_detail_benefit_1_desc,

            benefit2Icon = "🫐",
            benefit2TitleRes = R.string.mind_detail_benefit_2_title,
            benefit2DescRes = R.string.mind_detail_benefit_2_desc,

            benefit3Icon = "🛡️",
            benefit3TitleRes = R.string.mind_detail_benefit_3_title,
            benefit3DescRes = R.string.mind_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.mind_detail_meal_1_title,
            sampleMeal1DescRes = R.string.mind_detail_meal_1_desc,
            sampleMeal1KcalRes = R.string.mind_detail_meal_1_kcal,

            sampleMeal2TitleRes = R.string.mind_detail_meal_2_title,
            sampleMeal2DescRes = R.string.mind_detail_meal_2_desc,
            sampleMeal2KcalRes = R.string.mind_detail_meal_2_kcal,

            sampleMeal3TitleRes = R.string.mind_detail_meal_3_title,
            sampleMeal3DescRes = R.string.mind_detail_meal_3_desc,
            sampleMeal3KcalRes = R.string.mind_detail_meal_3_kcal,

            sampleMeal4TitleRes = R.string.mind_detail_meal_4_title,
            sampleMeal4DescRes = R.string.mind_detail_meal_4_desc,
            sampleMeal4KcalRes = R.string.mind_detail_meal_4_kcal,

            featuredFoodCount = 5,

            featuredFood1Icon = "🥬",
            featuredFood1Res = R.string.mind_detail_food_leafy_greens,

            featuredFood2Icon = "🫐",
            featuredFood2Res = R.string.mind_detail_food_berries,

            featuredFood3Icon = "🥜",
            featuredFood3Res = R.string.mind_detail_food_nuts,

            featuredFood4Icon = "🐟",
            featuredFood4Res = R.string.mind_detail_food_fish,

            featuredFood5Icon = "🫒",
            featuredFood5Res = R.string.mind_detail_food_olive_oil,

            featuredFood6Icon = "🌾",
            featuredFood6Res = R.string.mind_detail_food_whole_grains,

            forWhom1Res = R.string.mind_detail_for_whom_1,
            forWhom2Res = R.string.mind_detail_for_whom_2,
            forWhom3Res = R.string.mind_detail_for_whom_3,

            macroTitleRes = R.string.mind_detail_macro_title,
            macroCarbRes = R.string.mind_detail_macro_carb,
            macroProteinRes = R.string.mind_detail_macro_protein,
            macroFatRes = R.string.mind_detail_macro_fat,

            attentionIcon = "✅",
            attention1Res = R.string.mind_detail_attention_1,
            attention2Res = R.string.mind_detail_attention_2,
            attention3Res = R.string.mind_detail_attention_3
        ),
        DietPlanDetailFragment.PLAN_ELIMINATION to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_ELIMINATION,
            titleRes = R.string.elimination_detail_title,
            coverImageRes = R.drawable.elimination_diet_cover,

            badge1Res = R.string.elimination_detail_badge_calorie,
            badge2Res = R.string.elimination_detail_badge_duration,
            badge3Res = R.string.elimination_detail_badge_trigger_tracking,
            badge4Res = R.string.elimination_detail_badge_sensitivity,

            aboutIcon = "🔎",
            aboutDescRes = R.string.elimination_detail_about_desc,

            benefit1Icon = "🎯",
            benefit1TitleRes = R.string.elimination_detail_benefit_1_title,
            benefit1DescRes = R.string.elimination_detail_benefit_1_desc,

            benefit2Icon = "🌿",
            benefit2TitleRes = R.string.elimination_detail_benefit_2_title,
            benefit2DescRes = R.string.elimination_detail_benefit_2_desc,

            benefit3Icon = "📋",
            benefit3TitleRes = R.string.elimination_detail_benefit_3_title,
            benefit3DescRes = R.string.elimination_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.elimination_detail_meal_1_title,
            sampleMeal1DescRes = R.string.elimination_detail_meal_1_desc,
            sampleMeal1KcalRes = R.string.elimination_detail_meal_1_kcal,

            sampleMeal2TitleRes = R.string.elimination_detail_meal_2_title,
            sampleMeal2DescRes = R.string.elimination_detail_meal_2_desc,
            sampleMeal2KcalRes = R.string.elimination_detail_meal_2_kcal,

            sampleMeal3TitleRes = R.string.elimination_detail_meal_3_title,
            sampleMeal3DescRes = R.string.elimination_detail_meal_3_desc,
            sampleMeal3KcalRes = R.string.elimination_detail_meal_3_kcal,

            sampleMeal4TitleRes = R.string.elimination_detail_meal_4_title,
            sampleMeal4DescRes = R.string.elimination_detail_meal_4_desc,
            sampleMeal4KcalRes = R.string.elimination_detail_meal_4_kcal,

            featuredFoodCount = 5,

            featuredFood1Icon = "🍚",
            featuredFood1Res = R.string.elimination_detail_food_rice,

            featuredFood2Icon = "🐟",
            featuredFood2Res = R.string.elimination_detail_food_salmon,

            featuredFood3Icon = "🥒",
            featuredFood3Res = R.string.elimination_detail_food_zucchini,

            featuredFood4Icon = "🍐",
            featuredFood4Res = R.string.elimination_detail_food_pear,

            featuredFood5Icon = "🫒",
            featuredFood5Res = R.string.elimination_detail_food_olive_oil,

            featuredFood6Icon = "🍠",
            featuredFood6Res = R.string.elimination_detail_food_sweet_potato,

            forWhom1Res = R.string.elimination_detail_for_whom_1,
            forWhom2Res = R.string.elimination_detail_for_whom_2,
            forWhom3Res = R.string.elimination_detail_for_whom_3,

            macroTitleRes = R.string.elimination_detail_macro_title,
            macroCarbRes = R.string.elimination_detail_macro_carb,
            macroProteinRes = R.string.elimination_detail_macro_protein,
            macroFatRes = R.string.elimination_detail_macro_fat,

            attentionIcon = "⚠️",
            attention1Res = R.string.elimination_detail_attention_1,
            attention2Res = R.string.elimination_detail_attention_2,
            attention3Res = R.string.elimination_detail_attention_3
        ),
        DietPlanDetailFragment.PLAN_PLANT_BASED to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_PLANT_BASED,
            titleRes = R.string.plant_based_detail_title,
            coverImageRes = R.drawable.plant_based_diet_cover,

            badge1Res = R.string.plant_based_detail_badge_calorie,
            badge2Res = R.string.plant_based_detail_badge_duration,
            badge3Res = R.string.plant_based_detail_badge_plant_focused,
            badge4Res = R.string.plant_based_detail_badge_fiber_rich,

            aboutIcon = "🌱",
            aboutDescRes = R.string.plant_based_detail_about_desc,

            benefit1Icon = "🌿",
            benefit1TitleRes = R.string.plant_based_detail_benefit_1_title,
            benefit1DescRes = R.string.plant_based_detail_benefit_1_desc,

            benefit2Icon = "❤️",
            benefit2TitleRes = R.string.plant_based_detail_benefit_2_title,
            benefit2DescRes = R.string.plant_based_detail_benefit_2_desc,

            benefit3Icon = "🌍",
            benefit3TitleRes = R.string.plant_based_detail_benefit_3_title,
            benefit3DescRes = R.string.plant_based_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.plant_based_detail_meal_1_title,
            sampleMeal1DescRes = R.string.plant_based_detail_meal_1_desc,
            sampleMeal1KcalRes = R.string.plant_based_detail_meal_1_kcal,

            sampleMeal2TitleRes = R.string.plant_based_detail_meal_2_title,
            sampleMeal2DescRes = R.string.plant_based_detail_meal_2_desc,
            sampleMeal2KcalRes = R.string.plant_based_detail_meal_2_kcal,

            sampleMeal3TitleRes = R.string.plant_based_detail_meal_3_title,
            sampleMeal3DescRes = R.string.plant_based_detail_meal_3_desc,
            sampleMeal3KcalRes = R.string.plant_based_detail_meal_3_kcal,

            sampleMeal4TitleRes = R.string.plant_based_detail_meal_4_title,
            sampleMeal4DescRes = R.string.plant_based_detail_meal_4_desc,
            sampleMeal4KcalRes = R.string.plant_based_detail_meal_4_kcal,

            featuredFoodCount = 6,

            featuredFood1Icon = "🫘",
            featuredFood1Res = R.string.plant_based_detail_food_chickpeas,

            featuredFood2Icon = "🧡",
            featuredFood2Res = R.string.plant_based_detail_food_lentils,

            featuredFood3Icon = "◻️",
            featuredFood3Res = R.string.plant_based_detail_food_tofu,

            featuredFood4Icon = "🥑",
            featuredFood4Res = R.string.plant_based_detail_food_avocado,

            featuredFood5Icon = "🌾",
            featuredFood5Res = R.string.plant_based_detail_food_quinoa,

            featuredFood6Icon = "🥬",
            featuredFood6Res = R.string.plant_based_detail_food_leafy_greens,

            forWhom1Res = R.string.plant_based_detail_for_whom_1,
            forWhom2Res = R.string.plant_based_detail_for_whom_2,
            forWhom3Res = R.string.plant_based_detail_for_whom_3,

            macroTitleRes = R.string.plant_based_detail_macro_title,
            macroCarbRes = R.string.plant_based_detail_macro_carb,
            macroProteinRes = R.string.plant_based_detail_macro_protein,
            macroFatRes = R.string.plant_based_detail_macro_fat,

            attentionIcon = "✅",
            attention1Res = R.string.plant_based_detail_attention_1,
            attention2Res = R.string.plant_based_detail_attention_2,
            attention3Res = R.string.plant_based_detail_attention_3
        ),
        DietPlanDetailFragment.PLAN_WEIGHT_GAIN to DietPlanDetailUiModel(
            id = DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
            titleRes = R.string.weight_gain_detail_title,
            coverImageRes = R.drawable.weight_gain_diet_cover,

            badge1Res = R.string.weight_gain_detail_badge_calorie,
            badge2Res = R.string.weight_gain_detail_badge_duration,
            badge3Res = R.string.weight_gain_detail_badge_energy_dense,
            badge4Res = R.string.weight_gain_detail_badge_protein_support,

            aboutIcon = "⚡",
            aboutDescRes = R.string.weight_gain_detail_about_desc,

            benefit1Icon = "⚡",
            benefit1TitleRes = R.string.weight_gain_detail_benefit_1_title,
            benefit1DescRes = R.string.weight_gain_detail_benefit_1_desc,

            benefit2Icon = "💪",
            benefit2TitleRes = R.string.weight_gain_detail_benefit_2_title,
            benefit2DescRes = R.string.weight_gain_detail_benefit_2_desc,

            benefit3Icon = "⚖️",
            benefit3TitleRes = R.string.weight_gain_detail_benefit_3_title,
            benefit3DescRes = R.string.weight_gain_detail_benefit_3_desc,

            sampleMeal1TitleRes = R.string.weight_gain_detail_meal_1_title,
            sampleMeal1DescRes = R.string.weight_gain_detail_meal_1_desc,
            sampleMeal1KcalRes = R.string.weight_gain_detail_meal_1_kcal,

            sampleMeal2TitleRes = R.string.weight_gain_detail_meal_2_title,
            sampleMeal2DescRes = R.string.weight_gain_detail_meal_2_desc,
            sampleMeal2KcalRes = R.string.weight_gain_detail_meal_2_kcal,

            sampleMeal3TitleRes = R.string.weight_gain_detail_meal_3_title,
            sampleMeal3DescRes = R.string.weight_gain_detail_meal_3_desc,
            sampleMeal3KcalRes = R.string.weight_gain_detail_meal_3_kcal,

            sampleMeal4TitleRes = R.string.weight_gain_detail_meal_4_title,
            sampleMeal4DescRes = R.string.weight_gain_detail_meal_4_desc,
            sampleMeal4KcalRes = R.string.weight_gain_detail_meal_4_kcal,

            featuredFoodCount = 6,

            featuredFood1Icon = "🌾",
            featuredFood1Res = R.string.weight_gain_detail_food_oats,

            featuredFood2Icon = "🥜",
            featuredFood2Res = R.string.weight_gain_detail_food_peanut_butter,

            featuredFood3Icon = "🍌",
            featuredFood3Res = R.string.weight_gain_detail_food_banana,

            featuredFood4Icon = "🍚",
            featuredFood4Res = R.string.weight_gain_detail_food_rice,

            featuredFood5Icon = "🥚",
            featuredFood5Res = R.string.weight_gain_detail_food_egg,

            featuredFood6Icon = "🥜",
            featuredFood6Res = R.string.weight_gain_detail_food_nuts,

            forWhom1Res = R.string.weight_gain_detail_for_whom_1,
            forWhom2Res = R.string.weight_gain_detail_for_whom_2,
            forWhom3Res = R.string.weight_gain_detail_for_whom_3,

            macroTitleRes = R.string.weight_gain_detail_macro_title,
            macroCarbRes = R.string.weight_gain_detail_macro_carb,
            macroProteinRes = R.string.weight_gain_detail_macro_protein,
            macroFatRes = R.string.weight_gain_detail_macro_fat,

            attentionIcon = "✅",
            attention1Res = R.string.weight_gain_detail_attention_1,
            attention2Res = R.string.weight_gain_detail_attention_2,
            attention3Res = R.string.weight_gain_detail_attention_3
        )
    )

    fun get(planId: String): DietPlanDetailUiModel? {
        return all[planId]
    }
}

