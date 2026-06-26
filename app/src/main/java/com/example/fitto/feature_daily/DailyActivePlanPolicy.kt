package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog

object DailyActivePlanPolicy {

    fun isEmptyPlan(planId: String): Boolean {
        return planId == EMPTY_PLAN_ID
    }

    fun isCustomPlan(planId: String): Boolean {
        return planId.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)
    }

    fun isFastingPlan(planId: String): Boolean {
        return planId == DietPlanCatalog.PLAN_INTERMITTENT_FASTING ||
            planId.contains("fasting", ignoreCase = true) ||
            planId.contains("if", ignoreCase = true)
    }

    fun shouldUseBasicPlanRenderer(planId: String): Boolean {
        return isEmptyPlan(planId) ||
            planId == DietPlanCatalog.PLAN_INTERMITTENT_FASTING
    }

    private const val EMPTY_PLAN_ID = "none"
}
