package com.example.fitto.feature_dietplans.model

import androidx.annotation.StringRes

data class FastingPlanUiModel(
    val type: FastingPlanType,

    @StringRes val selectedPlanLabelRes: Int,
    @StringRes val selectedPlanRatioRes: Int,

    @StringRes val timeStartRes: Int,
    @StringRes val timeStartSubRes: Int,
    @StringRes val timeEatRes: Int,
    @StringRes val timeEatSubRes: Int,
    @StringRes val timeEndRes: Int,
    @StringRes val timeEndSubRes: Int,

    @StringRes val aboutDescRes: Int,

    @StringRes val benefit1TitleRes: Int,
    @StringRes val benefit1DescRes: Int,
    @StringRes val benefit2TitleRes: Int,
    @StringRes val benefit2DescRes: Int,
    @StringRes val benefit3TitleRes: Int,
    @StringRes val benefit3DescRes: Int,

    @StringRes val forWhomDescRes: Int,

    @StringRes val weeklyFlowDescRes: Int,

    @StringRes val faq1Res: Int,
    @StringRes val faq1AnswerRes: Int,
    @StringRes val faq2Res: Int,
    @StringRes val faq2AnswerRes: Int,
    @StringRes val faq3Res: Int,
    @StringRes val faq3AnswerRes: Int
)

enum class FastingPlanType {
    PLAN_12_12,
    PLAN_15_9,
    PLAN_16_8,
    PLAN_6_1,
    PLAN_5_2,
    PLAN_CUSTOM
}