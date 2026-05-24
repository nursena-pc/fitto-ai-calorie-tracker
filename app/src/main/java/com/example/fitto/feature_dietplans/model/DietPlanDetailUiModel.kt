package com.example.fitto.feature_dietplans.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DietPlanDetailUiModel(
    val id: String,

    @StringRes val titleRes: Int,
    @DrawableRes val coverImageRes: Int,

    @StringRes val badge1Res: Int,
    @StringRes val badge2Res: Int,
    @StringRes val badge3Res: Int,
    @StringRes val badge4Res: Int,

    val aboutIcon: String,
    @StringRes val aboutDescRes: Int,

    val benefit1Icon: String,
    @StringRes val benefit1TitleRes: Int,
    @StringRes val benefit1DescRes: Int,

    val benefit2Icon: String,
    @StringRes val benefit2TitleRes: Int,
    @StringRes val benefit2DescRes: Int,

    val benefit3Icon: String,
    @StringRes val benefit3TitleRes: Int,
    @StringRes val benefit3DescRes: Int,

    @StringRes val sampleMeal1TitleRes: Int,
    @StringRes val sampleMeal1DescRes: Int,
    @StringRes val sampleMeal1KcalRes: Int,

    @StringRes val sampleMeal2TitleRes: Int,
    @StringRes val sampleMeal2DescRes: Int,
    @StringRes val sampleMeal2KcalRes: Int,

    @StringRes val sampleMeal3TitleRes: Int,
    @StringRes val sampleMeal3DescRes: Int,
    @StringRes val sampleMeal3KcalRes: Int,

    @StringRes val sampleMeal4TitleRes: Int,
    @StringRes val sampleMeal4DescRes: Int,
    @StringRes val sampleMeal4KcalRes: Int,

    val featuredFoodCount: Int,

    val featuredFood1Icon: String,
    @StringRes val featuredFood1Res: Int,

    val featuredFood2Icon: String,
    @StringRes val featuredFood2Res: Int,

    val featuredFood3Icon: String,
    @StringRes val featuredFood3Res: Int,

    val featuredFood4Icon: String,
    @StringRes val featuredFood4Res: Int,

    val featuredFood5Icon: String,
    @StringRes val featuredFood5Res: Int,

    val featuredFood6Icon: String,
    @StringRes val featuredFood6Res: Int,

    @StringRes val forWhom1Res: Int,
    @StringRes val forWhom2Res: Int,
    @StringRes val forWhom3Res: Int,

    @StringRes val macroTitleRes: Int,
    @StringRes val macroCarbRes: Int,
    @StringRes val macroProteinRes: Int,
    @StringRes val macroFatRes: Int,

    val attentionIcon: String,
    @StringRes val attention1Res: Int,
    @StringRes val attention2Res: Int,
    @StringRes val attention3Res: Int
)