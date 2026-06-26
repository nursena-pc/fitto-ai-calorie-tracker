package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore

class CustomDietPlanDeletionAction(
    private val customPlanStore: CustomPlanStore,
    private val preferenceStore: PreferenceStore
) {

    fun delete(planId: String): CustomDietPlanDeletionResult {
        if (!isCustomPlanId(planId)) {
            return CustomDietPlanDeletionResult.NotCustomPlan
        }

        return runCatching {
            customPlanStore.deletePlan(planId)
            preferenceStore.removeFavorite(planId)
            preferenceStore.clearActivePlanIf(planId)
            preferenceStore.getActivePlanId()
        }.fold(
            onSuccess = { activePlanId ->
                CustomDietPlanDeletionResult.Deleted(activePlanId = activePlanId)
            },
            onFailure = { cause ->
                CustomDietPlanDeletionResult.Failed(cause)
            }
        )
    }

    interface CustomPlanStore {
        fun deletePlan(planId: String)
    }

    interface PreferenceStore {
        fun removeFavorite(planId: String)
        fun clearActivePlanIf(planId: String)
        fun getActivePlanId(): String?
    }

    companion object {
        fun from(
            customPlanStore: CustomDietPlanStore,
            preferenceStore: DietPlanPreferenceStore
        ): CustomDietPlanDeletionAction {
            return CustomDietPlanDeletionAction(
                customPlanStore = object : CustomPlanStore {
                    override fun deletePlan(planId: String) {
                        customPlanStore.deletePlan(planId)
                    }
                },
                preferenceStore = object : PreferenceStore {
                    override fun removeFavorite(planId: String) {
                        preferenceStore.removeFavorite(planId)
                    }

                    override fun clearActivePlanIf(planId: String) {
                        preferenceStore.clearActivePlanIf(planId)
                    }

                    override fun getActivePlanId(): String? {
                        return preferenceStore.getActivePlanId()
                    }
                }
            )
        }

        fun isCustomPlanId(planId: String): Boolean {
            return planId.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)
        }
    }
}

sealed class CustomDietPlanDeletionResult {
    data class Deleted(
        val activePlanId: String?
    ) : CustomDietPlanDeletionResult()

    object NotCustomPlan : CustomDietPlanDeletionResult()

    data class Failed(
        val cause: Throwable
    ) : CustomDietPlanDeletionResult()
}
