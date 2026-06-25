package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.DietPlanUiModel

object DietPlanTextResolver {

    fun title(
        plan: DietPlanUiModel,
        stringProvider: (Int) -> String,
        fallback: String
    ): String {
        return plan.titleText
            ?: plan.titleRes?.let(stringProvider)
            ?: fallback
    }

    fun description(
        plan: DietPlanUiModel,
        stringProvider: (Int) -> String,
        fallback: String
    ): String {
        return plan.descriptionText
            ?: plan.descriptionRes?.let(stringProvider)
            ?: fallback
    }
}
