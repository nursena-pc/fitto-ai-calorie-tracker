package com.example.fitto.feature_dietplans

class CreateDietPlanCompletionAction {

    fun draftSaved(wasUpdate: Boolean): CreateDietPlanCompletionEffect {
        return CreateDietPlanCompletionEffect(
            result = CreateDietPlanChangeResult(selectedTab = DietPlansFragment.TAB_DRAFTS),
            message = if (wasUpdate) {
                CreateDietPlanCompletionMessage.DRAFT_UPDATED
            } else {
                CreateDietPlanCompletionMessage.DRAFT_SAVED
            },
            closeScreen = true
        )
    }

    fun draftDeleted(): CreateDietPlanCompletionEffect {
        return CreateDietPlanCompletionEffect(
            result = CreateDietPlanChangeResult(selectedTab = DietPlansFragment.TAB_DRAFTS),
            message = CreateDietPlanCompletionMessage.DRAFT_DELETED,
            closeScreen = true
        )
    }

    fun planCreated(activePlanId: String): CreateDietPlanCompletionEffect {
        return CreateDietPlanCompletionEffect(
            result = CreateDietPlanChangeResult(
                activePlanId = activePlanId,
                selectedTab = DietPlansFragment.TAB_MY
            ),
            message = CreateDietPlanCompletionMessage.PLAN_CREATED,
            closeScreen = true
        )
    }
}

data class CreateDietPlanCompletionEffect(
    val result: CreateDietPlanChangeResult,
    val message: CreateDietPlanCompletionMessage,
    val closeScreen: Boolean
)

data class CreateDietPlanChangeResult(
    val activePlanId: String? = null,
    val selectedTab: String
)

enum class CreateDietPlanCompletionMessage {
    DRAFT_SAVED,
    DRAFT_UPDATED,
    DRAFT_DELETED,
    PLAN_CREATED
}
