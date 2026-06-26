package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DietPlanTextResolverTest {

    @Test
    fun title_prefersInlineTitleText() {
        val title = DietPlanTextResolver.title(
            plan = plan(
                titleText = "Custom title",
                titleRes = R.string.app_name
            ),
            stringProvider = { "Resource title" },
            fallback = "Fallback"
        )

        assertEquals("Custom title", title)
    }

    @Test
    fun title_usesStringProviderWhenTextIsMissing() {
        val title = DietPlanTextResolver.title(
            plan = plan(titleRes = R.string.app_name),
            stringProvider = { "Resource title" },
            fallback = "Fallback"
        )

        assertEquals("Resource title", title)
    }

    @Test
    fun title_usesFallbackWhenTitleFieldsAreMissing() {
        val title = DietPlanTextResolver.title(
            plan = plan(),
            stringProvider = { "Resource title" },
            fallback = "Fallback"
        )

        assertEquals("Fallback", title)
    }

    @Test
    fun description_usesSamePriorityAsTitle() {
        val inlineDescription = DietPlanTextResolver.description(
            plan = plan(
                descriptionText = "Custom description",
                descriptionRes = R.string.app_name
            ),
            stringProvider = { "Resource description" },
            fallback = "Fallback"
        )
        val resourceDescription = DietPlanTextResolver.description(
            plan = plan(descriptionRes = R.string.app_name),
            stringProvider = { "Resource description" },
            fallback = "Fallback"
        )

        assertEquals("Custom description", inlineDescription)
        assertEquals("Resource description", resourceDescription)
    }

    private fun plan(
        titleText: String? = null,
        titleRes: Int? = null,
        descriptionText: String? = null,
        descriptionRes: Int? = null
    ): DietPlanUiModel {
        return DietPlanUiModel(
            id = "plan",
            titleText = titleText,
            titleRes = titleRes,
            descriptionText = descriptionText,
            descriptionRes = descriptionRes,
            kcalPerDay = 1800,
            durationDays = 14
        )
    }
}
