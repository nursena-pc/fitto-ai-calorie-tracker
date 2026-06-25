package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import java.util.Locale

object DietPlanSearchResolver {

    fun filter(
        plans: List<DietPlanUiModel>,
        query: String,
        titleTextForPlan: (DietPlanUiModel) -> String,
        descriptionTextForPlan: (DietPlanUiModel) -> String
    ): List<DietPlanUiModel> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return plans

        return plans.filter { plan ->
            titleTextForPlan(plan).containsNormalized(normalizedQuery) ||
                descriptionTextForPlan(plan).containsNormalized(normalizedQuery)
        }
    }

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private fun String.containsNormalized(query: String): Boolean {
        return lowercase(Locale.ROOT).contains(query)
    }
}
