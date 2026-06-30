package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.feature_dietplans.data.CustomDietPlanStoreResolver
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDietPlanStoreResolverTest {

    @Test
    fun decodePlans_dropsInvalidPlansFiltersDraftsAndSortsByUpdatedAt() {
        val encodedPlans = CustomDietPlanStoreResolver.encodePlans(
            listOf(
                customPlan(id = "old", title = "Old", updatedAt = 100L),
                customPlan(id = "draft", title = "Draft", isDraft = true, updatedAt = 300L),
                customPlan(id = "new", title = "New", updatedAt = 200L)
            )
        )
        val rawJson = encodedPlans.dropLast(1) +
                ", {\"id\":\"missing_title\", \"updatedAt\":999}]"

        val visiblePlans = CustomDietPlanStoreResolver.decodePlans(
            rawJson = rawJson,
            includeDrafts = false
        )
        val allPlans = CustomDietPlanStoreResolver.decodePlans(
            rawJson = rawJson,
            includeDrafts = true
        )

        assertEquals(listOf("new", "old"), visiblePlans.map { it.id })
        assertEquals(listOf("draft", "new", "old"), allPlans.map { it.id })
    }

    @Test
    fun decodePlans_usesDefaultsAndCleansStringLists() {
        val rawJson = """
            [
              {
                "id": "custom_plan_1",
                "title": "Plan",
                "preferences": [" high protein ", "", " vegan"],
                "exclusions": [" onion ", "  "],
                "mealOrder": [" breakfast ", "dinner"],
                "createdAt": 10,
                "updatedAt": 20
              }
            ]
        """.trimIndent()

        val plan = CustomDietPlanStoreResolver.decodePlans(
            rawJson = rawJson,
            includeDrafts = true,
            nowProvider = { 42L }
        ).single()

        assertEquals(1800, plan.dailyCalories)
        assertEquals(4, plan.mealCount)
        assertEquals(30, plan.durationDays)
        assertEquals(listOf("high protein", "vegan"), plan.preferences)
        assertEquals(listOf("onion"), plan.exclusions)
        assertEquals(listOf("breakfast", "dinner"), plan.mealOrder)
    }

    @Test
    fun savePlan_movesPlanToFrontAndReplacesExistingCopy() {
        val result = CustomDietPlanStoreResolver.savePlan(
            existingPlans = listOf(
                customPlan(id = "a", title = "Old A"),
                customPlan(id = "b", title = "Plan B")
            ),
            plan = customPlan(id = "a", title = "New A")
        )

        assertEquals(listOf("a", "b"), result.map { it.id })
        assertEquals("New A", result.first().title)
    }

    @Test
    fun deletePlan_removesMatchingPlanOnly() {
        val result = CustomDietPlanStoreResolver.deletePlan(
            existingPlans = listOf(
                customPlan(id = "a"),
                customPlan(id = "b")
            ),
            planId = "a"
        )

        assertEquals(listOf("b"), result.map { it.id })
    }

    @Test
    fun hasPlanWithTitle_normalizesTurkishCharactersWhitespaceAndExcludedPlan() {
        val plans = listOf(
            customPlan(id = "a", title = "  Yağ   Yakımı  "),
            customPlan(id = "b", title = "Protein")
        )

        assertTrue(
            CustomDietPlanStoreResolver.hasPlanWithTitle(
                plans = plans,
                title = "yag yakimi"
            )
        )
        assertFalse(
            CustomDietPlanStoreResolver.hasPlanWithTitle(
                plans = plans,
                title = "yag yakimi",
                excludingPlanId = "a"
            )
        )
        assertFalse(
            CustomDietPlanStoreResolver.hasPlanWithTitle(
                plans = plans,
                title = "   "
            )
        )
    }

    @Test
    fun toDietPlanUiModel_mapsDraftAndDescriptionState() {
        val draft = CustomDietPlanStoreResolver.toDietPlanUiModel(
            plan = customPlan(
                id = "draft",
                title = "Draft plan",
                description = "",
                isDraft = true
            ),
            isFavorite = true
        )
        val published = CustomDietPlanStoreResolver.toDietPlanUiModel(
            plan = customPlan(
                id = "published",
                title = "Published plan",
                description = "Ready",
                isDraft = false
            ),
            isFavorite = false
        )

        assertEquals(R.string.diet_plan_tag_draft, draft.tagRes)
        assertEquals(R.string.diet_plan_tag_custom, published.tagRes)
        assertNull(draft.descriptionText)
        assertEquals("Ready", published.descriptionText)
        assertTrue(draft.isFavorite)
        assertFalse(published.isFavorite)
    }

    private fun customPlan(
        id: String = "custom_plan_1",
        title: String = "Plan",
        description: String = "Description",
        isDraft: Boolean = false,
        updatedAt: Long = 1L
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = id,
            title = title,
            description = description,
            goalType = "weight_loss",
            dailyCalories = 1800,
            mealCount = 4,
            durationDays = 30,
            isPublic = false,
            preferences = listOf("high_protein"),
            exclusions = listOf("onion"),
            mealOrder = listOf("breakfast", "lunch", "dinner"),
            isDraft = isDraft,
            createdAt = 0L,
            updatedAt = updatedAt
        )
    }
}
