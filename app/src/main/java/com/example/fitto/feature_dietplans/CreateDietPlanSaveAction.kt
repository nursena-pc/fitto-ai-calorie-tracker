package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel

class CreateDietPlanSaveAction(
    private val customPlanStore: CustomPlanStore,
    private val preferenceAction: PreferenceAction,
    private val nowProvider: () -> Long = System::currentTimeMillis
) {

    fun save(
        formState: CreateDietPlanFormState,
        existingPlan: CustomDietPlanLocalModel?,
        isDraft: Boolean
    ): CreateDietPlanSaveResult {
        val validation = CreateDietPlanSaveValidator.validate(
            planName = formState.planName,
            hasDuplicateTitle = formState.planName.isNotBlank() &&
                customPlanStore.hasPlanWithTitle(
                    title = formState.planName,
                    excludingPlanId = existingPlan?.id
                )
        )

        if (validation != CreateDietPlanSaveValidation.VALID) {
            return CreateDietPlanSaveResult.ValidationFailed(validation)
        }

        val plan = CreateDietPlanModelFactory.create(
            formState = formState,
            existingPlan = existingPlan,
            isDraft = isDraft,
            now = nowProvider()
        )

        customPlanStore.savePlan(plan)

        return if (isDraft) {
            CreateDietPlanSaveResult.DraftSaved(
                plan = plan,
                wasUpdate = existingPlan != null
            )
        } else {
            CreateDietPlanSaveResult.PlanCreated(
                plan = plan,
                goal = formState.goal,
                applyResult = preferenceAction.applyPlan(plan.id)
            )
        }
    }

    interface CustomPlanStore {
        fun hasPlanWithTitle(title: String, excludingPlanId: String? = null): Boolean
        fun savePlan(plan: CustomDietPlanLocalModel)
    }

    interface PreferenceAction {
        fun applyPlan(planId: String): DietPlanApplyResult
    }

    companion object {
        fun from(
            customPlanStore: CustomDietPlanStore,
            preferenceAction: DietPlanPreferenceAction
        ): CreateDietPlanSaveAction {
            return CreateDietPlanSaveAction(
                customPlanStore = object : CustomPlanStore {
                    override fun hasPlanWithTitle(
                        title: String,
                        excludingPlanId: String?
                    ): Boolean {
                        return customPlanStore.hasPlanWithTitle(
                            title = title,
                            excludingPlanId = excludingPlanId
                        )
                    }

                    override fun savePlan(plan: CustomDietPlanLocalModel) {
                        customPlanStore.savePlan(plan)
                    }
                },
                preferenceAction = object : PreferenceAction {
                    override fun applyPlan(planId: String): DietPlanApplyResult {
                        return preferenceAction.applyPlan(planId)
                    }
                }
            )
        }
    }
}

sealed class CreateDietPlanSaveResult {
    data class ValidationFailed(
        val validation: CreateDietPlanSaveValidation
    ) : CreateDietPlanSaveResult()

    data class DraftSaved(
        val plan: CustomDietPlanLocalModel,
        val wasUpdate: Boolean
    ) : CreateDietPlanSaveResult()

    data class PlanCreated(
        val plan: CustomDietPlanLocalModel,
        val goal: CreatePlanGoal,
        val applyResult: DietPlanApplyResult
    ) : CreateDietPlanSaveResult()
}
