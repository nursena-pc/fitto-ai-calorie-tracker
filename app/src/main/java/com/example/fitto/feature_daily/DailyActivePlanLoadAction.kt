package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.FastingPlanType
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel

class DailyActivePlanLoadAction(
    private val getActivePlanId: () -> String?,
    private val getActiveFastingType: () -> FastingPlanType?,
    private val getCustomFastingHours: () -> Int?,
    private val getCustomPlan: (planId: String) -> CustomDietPlanLocalModel?,
    private val createEmptyActivePlan: () -> ActiveDietPlanUiModel,
    private val createCatalogActivePlan: (
        planId: String,
        fastingType: FastingPlanType?,
        customFastingHours: Int?
    ) -> ActiveDietPlanUiModel,
    private val loadCustomPlan: suspend (CustomDietPlanLocalModel) -> DailyCustomPlanLoadResult,
    private val loadSeedPlan: suspend (
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ) -> DailyPlannedMealsUiModel?,
    private val loadCatalogPlan: (
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ) -> DailyPlannedMealsUiModel?
) {

    suspend fun load(targetCalories: Int): DailyActivePlanLoadResult {
        val activePlanId = getActivePlanId()

        if (activePlanId?.let(DailyActivePlanPolicy::isCustomPlan) == true) {
            val customPlan = getCustomPlan(activePlanId)
            if (customPlan != null) {
                val result = loadCustomPlan(customPlan)
                return if (result.dailyPlan.meals.isEmpty()) {
                    DailyActivePlanLoadResult.Basic(
                        displayPlan = result.activePlan,
                        activePlan = result.activePlan,
                        dailyPlan = result.dailyPlan
                    )
                } else {
                    DailyActivePlanLoadResult.Planned(
                        displayPlan = result.activePlan,
                        activePlan = result.activePlan,
                        dailyPlan = result.dailyPlan
                    )
                }
            }
        }

        val displayPlan = if (activePlanId == null) {
            createEmptyActivePlan()
        } else {
            createCatalogActivePlan(
                activePlanId,
                getActiveFastingType(),
                getCustomFastingHours()
            )
        }
        val activePlan = displayPlan.takeUnless { plan ->
            DailyActivePlanPolicy.isEmptyPlan(plan.id)
        }

        if (DailyActivePlanPolicy.shouldUseBasicPlanRenderer(displayPlan.id)) {
            return DailyActivePlanLoadResult.Basic(
                displayPlan = displayPlan,
                activePlan = activePlan
            )
        }

        val safeTargetCalories = targetCalories.coerceAtLeast(1)
        val dailyPlan = loadSeedPlan(displayPlan, safeTargetCalories)
            ?: loadCatalogPlan(displayPlan, safeTargetCalories)

        return if (dailyPlan == null) {
            DailyActivePlanLoadResult.Basic(
                displayPlan = displayPlan,
                activePlan = activePlan
            )
        } else {
            DailyActivePlanLoadResult.Planned(
                displayPlan = displayPlan,
                activePlan = displayPlan,
                dailyPlan = dailyPlan
            )
        }
    }
}

sealed interface DailyActivePlanLoadResult {
    val displayPlan: ActiveDietPlanUiModel
    val activePlan: ActiveDietPlanUiModel?
    val dailyPlan: DailyPlannedMealsUiModel?

    data class Basic(
        override val displayPlan: ActiveDietPlanUiModel,
        override val activePlan: ActiveDietPlanUiModel?,
        override val dailyPlan: DailyPlannedMealsUiModel? = null
    ) : DailyActivePlanLoadResult

    data class Planned(
        override val displayPlan: ActiveDietPlanUiModel,
        override val activePlan: ActiveDietPlanUiModel,
        override val dailyPlan: DailyPlannedMealsUiModel
    ) : DailyActivePlanLoadResult
}
