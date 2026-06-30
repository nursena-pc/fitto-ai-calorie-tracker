package com.example.fitto.feature_dietplans.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel

class CustomDietPlanStore(context: Context) {

    private val preferences = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getPlans(includeDrafts: Boolean = false): List<CustomDietPlanLocalModel> {
        val rawJson = preferences.getString(KEY_CUSTOM_PLANS, "[]").orEmpty()
        return CustomDietPlanStoreResolver.decodePlans(
            rawJson = rawJson,
            includeDrafts = includeDrafts
        )
    }

    fun getPlan(planId: String): CustomDietPlanLocalModel? {
        return getPlans(includeDrafts = true).firstOrNull { it.id == planId }
    }
    fun hasPlanWithTitle(
        title: String,
        excludingPlanId: String? = null
    ): Boolean {
        return CustomDietPlanStoreResolver.hasPlanWithTitle(
            plans = getPlans(includeDrafts = true),
            title = title,
            excludingPlanId = excludingPlanId
        )
    }
    fun savePlan(plan: CustomDietPlanLocalModel) {
        val updatedPlans = CustomDietPlanStoreResolver.savePlan(
            existingPlans = getPlans(includeDrafts = true),
            plan = plan
        )

        preferences.edit()
            .putString(KEY_CUSTOM_PLANS, CustomDietPlanStoreResolver.encodePlans(updatedPlans))
            .apply()
    }

    fun deletePlan(planId: String) {
        val updatedPlans = CustomDietPlanStoreResolver.deletePlan(
            existingPlans = getPlans(includeDrafts = true),
            planId = planId
        )

        preferences.edit()
            .putString(KEY_CUSTOM_PLANS, CustomDietPlanStoreResolver.encodePlans(updatedPlans))
            .apply()
    }

    fun toDietPlanUiModel(
        plan: CustomDietPlanLocalModel,
        isFavorite: Boolean
    ) = CustomDietPlanStoreResolver.toDietPlanUiModel(
        plan = plan,
        isFavorite = isFavorite
    )
    companion object {
        const val CUSTOM_PLAN_ID_PREFIX = "custom_plan_"

        private const val PREF_NAME = "fitto_custom_diet_plans"
        private const val KEY_CUSTOM_PLANS = "custom_plans"
    }
}