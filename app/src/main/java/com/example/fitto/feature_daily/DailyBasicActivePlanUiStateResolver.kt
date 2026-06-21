package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel

object DailyBasicActivePlanUiStateResolver {

    data class State(
        val planId: String,
        val title: String,
        val badge: String,
        val progress: Int,
        val content: Content
    )

    sealed interface Content {
        data object Fasting : Content

        data class Metrics(
            val primaryLabel: String,
            val primaryValue: String,
            val secondaryLabel: String,
            val secondaryValue: String
        ) : Content
    }

    fun resolve(activePlan: ActiveDietPlanUiModel): State {
        return State(
            planId = activePlan.id,
            title = activePlan.title,
            badge = activePlan.badge,
            progress = activePlan.progress,
            content = if (DailyActivePlanPolicy.isFastingPlan(activePlan.id)) {
                Content.Fasting
            } else {
                Content.Metrics(
                    primaryLabel = activePlan.primaryLabel,
                    primaryValue = activePlan.primaryValue,
                    secondaryLabel = activePlan.secondaryLabel,
                    secondaryValue = activePlan.secondaryValue
                )
            }
        )
    }
}
