package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel

object CreateDietPlanModelFactory {

    fun create(
        formState: CreateDietPlanFormState,
        existingPlan: CustomDietPlanLocalModel?,
        isDraft: Boolean,
        now: Long
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = existingPlan?.id ?: "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$now",
            title = formState.planName,
            description = formState.description,
            goalType = formState.goal.name,
            dailyCalories = formState.dailyCalories,
            mealCount = formState.mealTypes.size,
            durationDays = formState.durationDays,
            isPublic = formState.privacy == CreatePlanPrivacy.PUBLIC,
            preferences = formState.preferences,
            exclusions = formState.exclusions,
            mealOrder = formState.mealTypes.map { mealType -> mealType.argumentValue },
            isDraft = isDraft,
            createdAt = existingPlan?.createdAt ?: now,
            updatedAt = now
        )
    }
}
