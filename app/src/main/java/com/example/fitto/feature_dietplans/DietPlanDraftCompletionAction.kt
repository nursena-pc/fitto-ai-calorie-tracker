package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel

class DietPlanDraftCompletionAction(
    private val customPlanStore: CustomPlanStore,
    private val nowProvider: () -> Long = System::currentTimeMillis
) {

    fun complete(planId: String): DietPlanDraftCompletionResult {
        val draft = customPlanStore.getPlan(planId)
            ?.takeIf { plan -> plan.isDraft }
            ?: return DietPlanDraftCompletionResult.NotCompleted

        val completedPlan = draft.copy(
            isDraft = false,
            updatedAt = nowProvider()
        )

        customPlanStore.savePlan(completedPlan)

        return DietPlanDraftCompletionResult.Completed(completedPlan)
    }

    interface CustomPlanStore {
        fun getPlan(planId: String): CustomDietPlanLocalModel?
        fun savePlan(plan: CustomDietPlanLocalModel)
    }

    companion object {
        fun from(customPlanStore: CustomDietPlanStore): DietPlanDraftCompletionAction {
            return DietPlanDraftCompletionAction(
                customPlanStore = object : CustomPlanStore {
                    override fun getPlan(planId: String): CustomDietPlanLocalModel? {
                        return customPlanStore.getPlan(planId)
                    }

                    override fun savePlan(plan: CustomDietPlanLocalModel) {
                        customPlanStore.savePlan(plan)
                    }
                }
            )
        }
    }
}

sealed class DietPlanDraftCompletionResult {
    data class Completed(
        val plan: CustomDietPlanLocalModel
    ) : DietPlanDraftCompletionResult()

    object NotCompleted : DietPlanDraftCompletionResult()
}
