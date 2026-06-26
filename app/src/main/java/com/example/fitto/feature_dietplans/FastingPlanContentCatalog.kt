package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.feature_dietplans.model.FastingPlanType
import com.example.fitto.feature_dietplans.model.FastingPlanUiModel

object FastingPlanContentCatalog {
    private val plans: Map<FastingPlanType, FastingPlanUiModel> = mapOf(
        FastingPlanType.PLAN_12_12 to FastingPlanUiModel(
            type = FastingPlanType.PLAN_12_12,
            selectedPlanLabelRes = R.string.if_plan_12_12_selected_label,
            selectedPlanRatioRes = R.string.if_plan_12_12_selected_ratio,

            timeStartRes = R.string.if_plan_12_12_time_start,
            timeStartSubRes = R.string.if_plan_12_12_time_start_sub,
            timeEatRes = R.string.if_plan_12_12_time_eat,
            timeEatSubRes = R.string.if_plan_12_12_time_eat_sub,
            timeEndRes = R.string.if_plan_12_12_time_end,
            timeEndSubRes = R.string.if_plan_12_12_time_end_sub,

            aboutDescRes = R.string.if_plan_12_12_about_desc,

            benefit1TitleRes = R.string.if_plan_12_12_benefit_1_title,
            benefit1DescRes = R.string.if_plan_12_12_benefit_1_desc,
            benefit2TitleRes = R.string.if_plan_12_12_benefit_2_title,
            benefit2DescRes = R.string.if_plan_12_12_benefit_2_desc,
            benefit3TitleRes = R.string.if_plan_12_12_benefit_3_title,
            benefit3DescRes = R.string.if_plan_12_12_benefit_3_desc,

            forWhomDescRes = R.string.if_plan_12_12_for_whom_desc,
            weeklyFlowDescRes = R.string.if_plan_12_12_weekly_flow_desc,

            faq1Res = R.string.if_plan_12_12_faq_1,
            faq1AnswerRes = R.string.if_plan_12_12_faq_1_answer,
            faq2Res = R.string.if_plan_12_12_faq_2,
            faq2AnswerRes = R.string.if_plan_12_12_faq_2_answer,
            faq3Res = R.string.if_plan_12_12_faq_3,
            faq3AnswerRes = R.string.if_plan_12_12_faq_3_answer
        ),
        FastingPlanType.PLAN_15_9 to FastingPlanUiModel(
            type = FastingPlanType.PLAN_15_9,
            selectedPlanLabelRes = R.string.if_plan_15_9_selected_label,
            selectedPlanRatioRes = R.string.if_plan_15_9_selected_ratio,

            timeStartRes = R.string.if_plan_15_9_time_start,
            timeStartSubRes = R.string.if_plan_15_9_time_start_sub,
            timeEatRes = R.string.if_plan_15_9_time_eat,
            timeEatSubRes = R.string.if_plan_15_9_time_eat_sub,
            timeEndRes = R.string.if_plan_15_9_time_end,
            timeEndSubRes = R.string.if_plan_15_9_time_end_sub,

            aboutDescRes = R.string.if_plan_15_9_about_desc,

            benefit1TitleRes = R.string.if_plan_15_9_benefit_1_title,
            benefit1DescRes = R.string.if_plan_15_9_benefit_1_desc,
            benefit2TitleRes = R.string.if_plan_15_9_benefit_2_title,
            benefit2DescRes = R.string.if_plan_15_9_benefit_2_desc,
            benefit3TitleRes = R.string.if_plan_15_9_benefit_3_title,
            benefit3DescRes = R.string.if_plan_15_9_benefit_3_desc,

            forWhomDescRes = R.string.if_plan_15_9_for_whom_desc,
            weeklyFlowDescRes = R.string.if_plan_15_9_weekly_flow_desc,

            faq1Res = R.string.if_plan_15_9_faq_1,
            faq1AnswerRes = R.string.if_plan_15_9_faq_1_answer,
            faq2Res = R.string.if_plan_15_9_faq_2,
            faq2AnswerRes = R.string.if_plan_15_9_faq_2_answer,
            faq3Res = R.string.if_plan_15_9_faq_3,
            faq3AnswerRes = R.string.if_plan_15_9_faq_3_answer
        ),
        FastingPlanType.PLAN_6_1 to FastingPlanUiModel(
            type = FastingPlanType.PLAN_6_1,
            selectedPlanLabelRes = R.string.if_plan_6_1_selected_label,
            selectedPlanRatioRes = R.string.if_plan_6_1_selected_ratio,

            timeStartRes = R.string.if_plan_6_1_time_start,
            timeStartSubRes = R.string.if_plan_6_1_time_start_sub,
            timeEatRes = R.string.if_plan_6_1_time_eat,
            timeEatSubRes = R.string.if_plan_6_1_time_eat_sub,
            timeEndRes = R.string.if_plan_6_1_time_end,
            timeEndSubRes = R.string.if_plan_6_1_time_end_sub,

            aboutDescRes = R.string.if_plan_6_1_about_desc,

            benefit1TitleRes = R.string.if_plan_6_1_benefit_1_title,
            benefit1DescRes = R.string.if_plan_6_1_benefit_1_desc,
            benefit2TitleRes = R.string.if_plan_6_1_benefit_2_title,
            benefit2DescRes = R.string.if_plan_6_1_benefit_2_desc,
            benefit3TitleRes = R.string.if_plan_6_1_benefit_3_title,
            benefit3DescRes = R.string.if_plan_6_1_benefit_3_desc,

            forWhomDescRes = R.string.if_plan_6_1_for_whom_desc,
            weeklyFlowDescRes = R.string.if_plan_6_1_weekly_flow_desc,

            faq1Res = R.string.if_plan_6_1_faq_1,
            faq1AnswerRes = R.string.if_plan_6_1_faq_1_answer,
            faq2Res = R.string.if_plan_6_1_faq_2,
            faq2AnswerRes = R.string.if_plan_6_1_faq_2_answer,
            faq3Res = R.string.if_plan_6_1_faq_3,
            faq3AnswerRes = R.string.if_plan_6_1_faq_3_answer
        ),
        FastingPlanType.PLAN_5_2 to FastingPlanUiModel(
            type = FastingPlanType.PLAN_5_2,
            selectedPlanLabelRes = R.string.if_plan_5_2_selected_label,
            selectedPlanRatioRes = R.string.if_plan_5_2_selected_ratio,

            timeStartRes = R.string.if_plan_5_2_time_start,
            timeStartSubRes = R.string.if_plan_5_2_time_start_sub,
            timeEatRes = R.string.if_plan_5_2_time_eat,
            timeEatSubRes = R.string.if_plan_5_2_time_eat_sub,
            timeEndRes = R.string.if_plan_5_2_time_end,
            timeEndSubRes = R.string.if_plan_5_2_time_end_sub,

            aboutDescRes = R.string.if_plan_5_2_about_desc,

            benefit1TitleRes = R.string.if_plan_5_2_benefit_1_title,
            benefit1DescRes = R.string.if_plan_5_2_benefit_1_desc,
            benefit2TitleRes = R.string.if_plan_5_2_benefit_2_title,
            benefit2DescRes = R.string.if_plan_5_2_benefit_2_desc,
            benefit3TitleRes = R.string.if_plan_5_2_benefit_3_title,
            benefit3DescRes = R.string.if_plan_5_2_benefit_3_desc,

            forWhomDescRes = R.string.if_plan_5_2_for_whom_desc,
            weeklyFlowDescRes = R.string.if_plan_5_2_weekly_flow_desc,

            faq1Res = R.string.if_plan_5_2_faq_1,
            faq1AnswerRes = R.string.if_plan_5_2_faq_1_answer,
            faq2Res = R.string.if_plan_5_2_faq_2,
            faq2AnswerRes = R.string.if_plan_5_2_faq_2_answer,
            faq3Res = R.string.if_plan_5_2_faq_3,
            faq3AnswerRes = R.string.if_plan_5_2_faq_3_answer
        ),
        FastingPlanType.PLAN_16_8 to FastingPlanUiModel(
            type = FastingPlanType.PLAN_16_8,
            selectedPlanLabelRes = R.string.if_detail_selected_plan,
            selectedPlanRatioRes = R.string.if_detail_selected_plan_ratio,

            timeStartRes = R.string.if_detail_time_start,
            timeStartSubRes = R.string.if_detail_time_start_sub,
            timeEatRes = R.string.if_detail_time_eat,
            timeEatSubRes = R.string.if_detail_time_eat_sub,
            timeEndRes = R.string.if_detail_time_end,
            timeEndSubRes = R.string.if_detail_time_end_sub,

            aboutDescRes = R.string.if_detail_about_desc,

            benefit1TitleRes = R.string.if_detail_benefit_1_title,
            benefit1DescRes = R.string.if_detail_benefit_1_desc,
            benefit2TitleRes = R.string.if_detail_benefit_2_title,
            benefit2DescRes = R.string.if_detail_benefit_2_desc,
            benefit3TitleRes = R.string.if_detail_benefit_3_title,
            benefit3DescRes = R.string.if_detail_benefit_3_desc,

            forWhomDescRes = R.string.if_detail_for_whom_desc,
            weeklyFlowDescRes = R.string.if_detail_weekly_flow_desc,

            faq1Res = R.string.if_detail_faq_1,
            faq1AnswerRes = R.string.if_detail_faq_1_answer,
            faq2Res = R.string.if_detail_faq_2,
            faq2AnswerRes = R.string.if_detail_faq_2_answer,
            faq3Res = R.string.if_detail_faq_3,
            faq3AnswerRes = R.string.if_detail_faq_3_answer
        )
    )

    fun get(planType: FastingPlanType): FastingPlanUiModel? {
        return plans[planType]
    }
}
