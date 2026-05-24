    package com.example.fitto.feature_dietplans

    import android.os.Bundle
    import android.view.View
    import android.widget.Toast
    import androidx.fragment.app.Fragment
    import com.example.fitto.R
    import com.example.fitto.databinding.FragmentDietPlanDetailBinding
    import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
    import com.example.fitto.feature_dietplans.model.DietPlanDetailUiModel
    import kotlin.getValue

    class DietPlanDetailFragment : Fragment(R.layout.fragment_diet_plan_detail) {

        private var _binding: FragmentDietPlanDetailBinding? = null
        private val binding get() = _binding!!

        private lateinit var preferenceStore: DietPlanPreferenceStore
        private var currentPlanId: String = PLAN_MEDITERRANEAN

        private val dietDetails by lazy {
            mapOf(
                PLAN_MEDITERRANEAN to DietPlanDetailUiModel(
                    id = PLAN_MEDITERRANEAN,
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

                PLAN_KETOGENIC to DietPlanDetailUiModel(
                    id = PLAN_KETOGENIC,
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
                PLAN_DASH to DietPlanDetailUiModel(
                    id = PLAN_DASH,
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
                PLAN_MIND to DietPlanDetailUiModel(
                    id = PLAN_MIND,
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
                PLAN_ELIMINATION to DietPlanDetailUiModel(
                    id = PLAN_ELIMINATION,
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
                PLAN_PLANT_BASED to DietPlanDetailUiModel(
                    id = PLAN_PLANT_BASED,
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
                PLAN_WEIGHT_GAIN to DietPlanDetailUiModel(
                    id = PLAN_WEIGHT_GAIN,
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
        }


        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            _binding = FragmentDietPlanDetailBinding.bind(view)
            preferenceStore = DietPlanPreferenceStore(requireContext())

            currentPlanId = arguments?.getString(ARG_PLAN_ID) ?: PLAN_MEDITERRANEAN
            setupClicks()
            bindPlanDetail(currentPlanId)
            renderSaveState()
        }
        private fun toggleSaveCurrentPlan() {
            val detail = dietDetails[currentPlanId] ?: return
            val isSavedNow = preferenceStore.toggleFavorite(currentPlanId)

            renderSaveState()

            Toast.makeText(
                requireContext(),
                getString(
                    if (isSavedNow) {
                        R.string.diet_plans_favorite_added_message
                    } else {
                        R.string.diet_plans_favorite_removed_message
                    },
                    getString(detail.titleRes)
                ),
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.setFragmentResult(
                DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
                Bundle().apply {
                    putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, preferenceStore.getActivePlanId())
                }
            )
        }

        private fun renderSaveState() = with(binding) {
            val isSaved = preferenceStore.isFavorite(currentPlanId)

            savePlanButton.text = getString(
                if (isSaved) R.string.diet_plan_detail_saved_plan
                else R.string.diet_plan_detail_save_plan
            )

            savePlanButton.alpha = if (isSaved) 0.86f else 1f
        }
        private fun bindPlanDetail(planId: String) = with(binding) {
            val detail = dietDetails[planId] ?: return@with

            heroCoverImage.setImageResource(detail.coverImageRes)
            heroTitle.setText(detail.titleRes)

            badge1Text.setText(detail.badge1Res)
            badge2Text.setText(detail.badge2Res)
            badge3Text.setText(detail.badge3Res)
            badge4Text.setText(detail.badge4Res)

            aboutTitleIconText.text = detail.aboutIcon
            aboutDescText.setText(detail.aboutDescRes)

            benefit1IconText.text = detail.benefit1Icon
            benefit1TitleText.setText(detail.benefit1TitleRes)
            benefit1DescText.setText(detail.benefit1DescRes)

            benefit2IconText.text = detail.benefit2Icon
            benefit2TitleText.setText(detail.benefit2TitleRes)
            benefit2DescText.setText(detail.benefit2DescRes)

            benefit3IconText.text = detail.benefit3Icon
            benefit3TitleText.setText(detail.benefit3TitleRes)
            benefit3DescText.setText(detail.benefit3DescRes)

            sampleMeal1TitleText.setText(detail.sampleMeal1TitleRes)
            sampleMeal1DescText.setText(detail.sampleMeal1DescRes)
            sampleMeal1KcalText.setText(detail.sampleMeal1KcalRes)

            sampleMeal2TitleText.setText(detail.sampleMeal2TitleRes)
            sampleMeal2DescText.setText(detail.sampleMeal2DescRes)
            sampleMeal2KcalText.setText(detail.sampleMeal2KcalRes)

            sampleMeal3TitleText.setText(detail.sampleMeal3TitleRes)
            sampleMeal3DescText.setText(detail.sampleMeal3DescRes)
            sampleMeal3KcalText.setText(detail.sampleMeal3KcalRes)

            sampleMeal4TitleText.setText(detail.sampleMeal4TitleRes)
            sampleMeal4DescText.setText(detail.sampleMeal4DescRes)
            sampleMeal4KcalText.setText(detail.sampleMeal4KcalRes)

            featuredFood1IconText.text = detail.featuredFood1Icon
            featuredFood1Text.setText(detail.featuredFood1Res)

            featuredFood2IconText.text = detail.featuredFood2Icon
            featuredFood2Text.setText(detail.featuredFood2Res)

            featuredFood3IconText.text = detail.featuredFood3Icon
            featuredFood3Text.setText(detail.featuredFood3Res)

            featuredFood4IconText.text = detail.featuredFood4Icon
            featuredFood4Text.setText(detail.featuredFood4Res)

            featuredFood5IconText.text = detail.featuredFood5Icon
            featuredFood5Text.setText(detail.featuredFood5Res)

            featuredFood6IconText.text = detail.featuredFood6Icon
            featuredFood6Text.setText(detail.featuredFood6Res)

            applyFeaturedFoodCount(detail.featuredFoodCount)

            forWhom1Text.setText(detail.forWhom1Res)
            forWhom2Text.setText(detail.forWhom2Res)
            forWhom3Text.setText(detail.forWhom3Res)

            macroTitleText.setText(detail.macroTitleRes)
            macroCarbText.setText(detail.macroCarbRes)
            macroProteinText.setText(detail.macroProteinRes)
            macroFatText.setText(detail.macroFatRes)

            attentionTitleIconText.text = detail.attentionIcon
            attention1Text.setText(detail.attention1Res)
            attention2Text.setText(detail.attention2Res)
            attention3Text.setText(detail.attention3Res)
        }
        private fun applyFeaturedFoodVisibility(planId: String) = with(binding) {
            val showSixFoods = planId == PLAN_KETOGENIC

            featuredFood5Card.visibility = if (showSixFoods) View.VISIBLE else View.GONE
            featuredFood6Card.visibility = if (showSixFoods) View.VISIBLE else View.GONE
        }
        private fun applyFeaturedFoodCount(count: Int) = with(binding) {
            featuredFood5Card.visibility = if (count >= 5) View.VISIBLE else View.GONE
            featuredFood6Card.visibility = if (count >= 6) View.VISIBLE else View.GONE
        }
        private fun applyDietSpecificIcons(planId: String) = with(binding) {
            when (planId) {
                PLAN_KETOGENIC -> {
                    aboutTitleIconText.text = "🥑"
                    attentionTitleIconText.text = "⚠️"

                    benefit1IconText.text = "🔥"   // Yağ Yakımı
                    benefit2IconText.text = "🥑"   // Tokluk Hissi
                    benefit3IconText.text = "📉"   // Düşük Karbonhidrat

                    featuredFood1IconText.text = "🥑"
                    featuredFood2IconText.text = "🥚"
                    featuredFood3IconText.text = "🐟"
                    featuredFood4IconText.text = "🧀"
                    featuredFood5IconText.text = "🫒"
                    featuredFood6IconText.text = "🥜"
                }
            }
        }

        private fun setupClicks() = with(binding) {
            backButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }


            applyPlanButton.setOnClickListener {
                applyCurrentPlan()
            }
            bottomApplyPlanButton.setOnClickListener {
                applyCurrentPlan()
            }
            savePlanButton.setOnClickListener {
                toggleSaveCurrentPlan()
            }
        }


        private fun applyCurrentPlan() {
            val detail = dietDetails[currentPlanId] ?: return
            preferenceStore.saveActivePlan(currentPlanId)
            preferenceStore.applyActivePlanToday()
            parentFragmentManager.setFragmentResult(
                DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
                Bundle().apply {
                    putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, currentPlanId)
                }
            )

            Toast.makeText(
                requireContext(),
                getString(R.string.diet_plans_apply_success_message, getString(detail.titleRes)),
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
        }

        override fun onDestroyView() {
            _binding = null
            super.onDestroyView()
        }
        companion object {
            private const val ARG_PLAN_ID = "arg_plan_id"
            const val PLAN_DASH = "dash"
            const val PLAN_MEDITERRANEAN = "mediterranean"
            const val PLAN_KETOGENIC = "ketogenic"
            const val PLAN_MIND = "mind"
            const val PLAN_ELIMINATION = "elimination"
            const val PLAN_PLANT_BASED = "plant_based"
            const val PLAN_WEIGHT_GAIN = "weight_gain"

            fun newInstance(planId: String): DietPlanDetailFragment {
                return DietPlanDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PLAN_ID, planId)
                    }
                }
            }
        }
    }