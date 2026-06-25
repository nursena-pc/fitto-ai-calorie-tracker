package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanSearchResolverTest {

    @Test
    fun filter_returnsOriginalPlansWhenQueryIsBlank() {
        val plans = listOf(
            plan(id = "mediterranean", title = "Mediterranean"),
            plan(id = "dash", title = "DASH")
        )

        assertEquals(
            plans,
            DietPlanSearchResolver.filter(
                plans = plans,
                query = "   ",
                titleTextForPlan = { plan -> plan.titleText.orEmpty() },
                descriptionTextForPlan = { plan -> plan.descriptionText.orEmpty() }
            )
        )
    }

    @Test
    fun filter_matchesTitleIgnoringCaseAndWhitespace() {
        val result = DietPlanSearchResolver.filter(
            plans = listOf(
                plan(id = "mediterranean", title = "Mediterranean Diet"),
                plan(id = "dash", title = "DASH")
            ),
            query = "  mediterranean ",
            titleTextForPlan = { plan -> plan.titleText.orEmpty() },
            descriptionTextForPlan = { plan -> plan.descriptionText.orEmpty() }
        )

        assertEquals(listOf("mediterranean"), result.map(DietPlanUiModel::id))
    }

    @Test
    fun filter_matchesDescriptionText() {
        val result = DietPlanSearchResolver.filter(
            plans = listOf(
                plan(id = "keto", title = "Keto", description = "Low carbohydrate plan"),
                plan(id = "mind", title = "MIND", description = "Brain friendly plan")
            ),
            query = "brain",
            titleTextForPlan = { plan -> plan.titleText.orEmpty() },
            descriptionTextForPlan = { plan -> plan.descriptionText.orEmpty() }
        )

        assertEquals(listOf("mind"), result.map(DietPlanUiModel::id))
    }

    @Test
    fun filter_returnsEmptyListWhenNothingMatches() {
        val result = DietPlanSearchResolver.filter(
            plans = listOf(plan(id = "dash", title = "DASH")),
            query = "ketogenic",
            titleTextForPlan = { plan -> plan.titleText.orEmpty() },
            descriptionTextForPlan = { plan -> plan.descriptionText.orEmpty() }
        )

        assertTrue(result.isEmpty())
    }

    private fun plan(
        id: String,
        title: String,
        description: String = ""
    ): DietPlanUiModel {
        return DietPlanUiModel(
            id = id,
            titleText = title,
            descriptionText = description,
            kcalPerDay = 1800,
            durationDays = 14
        )
    }
}
