package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.model.FastingPlanType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivePlanLoadActionTest {

    @Test
    fun `no active plan returns empty basic state`() = runBlocking {
        val emptyPlan = dailyTestActivePlan(id = "none")
        val action = action(
            activePlanId = null,
            emptyPlan = emptyPlan
        )

        val result = action.load(1800)

        assertTrue(result is DailyActivePlanLoadResult.Basic)
        assertEquals(emptyPlan, result.displayPlan)
        assertNull(result.activePlan)
        assertNull(result.dailyPlan)
    }

    @Test
    fun `fasting plan uses basic renderer without loading meals`() = runBlocking {
        val fastingPlan = dailyTestActivePlan(id = DietPlanCatalog.PLAN_INTERMITTENT_FASTING)
        var seedLoadCount = 0
        val action = action(
            activePlanId = DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
            catalogActivePlan = fastingPlan,
            loadSeedPlan = { _, _ ->
                seedLoadCount += 1
                null
            }
        )

        val result = action.load(1800)

        assertTrue(result is DailyActivePlanLoadResult.Basic)
        assertEquals(fastingPlan, result.activePlan)
        assertEquals(0, seedLoadCount)
    }

    @Test
    fun `custom plan with meals returns planned state`() = runBlocking {
        val customPlan = dailyTestCustomPlan()
        val activePlan = dailyTestActivePlan(id = customPlan.id)
        val dailyPlan = dailyTestMealPlan(planId = customPlan.id)
        val action = action(
            activePlanId = customPlan.id,
            customPlan = customPlan,
            customResult = DailyCustomPlanLoadResult(activePlan, dailyPlan)
        )

        val result = action.load(1900)

        assertTrue(result is DailyActivePlanLoadResult.Planned)
        assertEquals(activePlan, result.activePlan)
        assertEquals(dailyPlan, result.dailyPlan)
    }

    @Test
    fun `custom plan with no meals keeps generated daily plan in basic state`() = runBlocking {
        val customPlan = dailyTestCustomPlan()
        val activePlan = dailyTestActivePlan(id = customPlan.id)
        val dailyPlan = dailyTestMealPlan(
            planId = customPlan.id,
            meals = emptyList(),
            nextMeal = null
        )
        val action = action(
            activePlanId = customPlan.id,
            customPlan = customPlan,
            customResult = DailyCustomPlanLoadResult(activePlan, dailyPlan)
        )

        val result = action.load(1900)

        assertTrue(result is DailyActivePlanLoadResult.Basic)
        assertSame(dailyPlan, result.dailyPlan)
        assertEquals(activePlan, result.activePlan)
    }

    @Test
    fun `seed plan is preferred over catalog fallback`() = runBlocking {
        val activePlan = dailyTestActivePlan()
        val seedPlan = dailyTestMealPlan(planId = activePlan.id)
        var catalogLoadCount = 0
        val action = action(
            activePlanId = activePlan.id,
            catalogActivePlan = activePlan,
            loadSeedPlan = { _, targetCalories ->
                assertEquals(1, targetCalories)
                seedPlan
            },
            loadCatalogPlan = { _, _ ->
                catalogLoadCount += 1
                null
            }
        )

        val result = action.load(0)

        assertTrue(result is DailyActivePlanLoadResult.Planned)
        assertSame(seedPlan, result.dailyPlan)
        assertEquals(0, catalogLoadCount)
    }

    @Test
    fun `catalog plan is used when room plan is unavailable`() = runBlocking {
        val activePlan = dailyTestActivePlan()
        val catalogPlan = dailyTestMealPlan(planId = activePlan.id)
        val action = action(
            activePlanId = activePlan.id,
            catalogActivePlan = activePlan,
            loadSeedPlan = { _, _ -> null },
            loadCatalogPlan = { _, targetCalories ->
                assertEquals(2000, targetCalories)
                catalogPlan
            }
        )

        val result = action.load(2000)

        assertTrue(result is DailyActivePlanLoadResult.Planned)
        assertSame(catalogPlan, result.dailyPlan)
    }

    @Test
    fun `missing custom plan preserves catalog fallback behavior`() = runBlocking {
        val missingCustomId = "custom_plan_missing"
        val fallbackPlan = dailyTestActivePlan(id = "mediterranean")
        var requestedCatalogPlanId: String? = null
        val action = action(
            activePlanId = missingCustomId,
            customPlan = null,
            catalogActivePlan = fallbackPlan,
            createCatalogActivePlan = { planId, fastingType, customHours ->
                requestedCatalogPlanId = planId
                assertEquals(FastingPlanType.PLAN_16_8, fastingType)
                assertEquals(17, customHours)
                fallbackPlan
            },
            loadSeedPlan = { _, _ -> null },
            loadCatalogPlan = { _, _ -> null }
        )

        val result = action.load(1800)

        assertEquals(missingCustomId, requestedCatalogPlanId)
        assertTrue(result is DailyActivePlanLoadResult.Basic)
        assertEquals(fallbackPlan, result.activePlan)
    }

    private fun action(
        activePlanId: String?,
        emptyPlan: com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel = dailyTestActivePlan(
            id = "none"
        ),
        catalogActivePlan: com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel = dailyTestActivePlan(),
        customPlan: com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel? = null,
        customResult: DailyCustomPlanLoadResult = DailyCustomPlanLoadResult(
            dailyTestActivePlan(id = "custom_plan_1"),
            dailyTestMealPlan(planId = "custom_plan_1")
        ),
        createCatalogActivePlan: (
            String,
            FastingPlanType?,
            Int?
        ) -> com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel = { _, _, _ ->
            catalogActivePlan
        },
        loadSeedPlan: suspend (
            com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel,
            Int
        ) -> com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel? = { _, _ -> null },
        loadCatalogPlan: (
            com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel,
            Int
        ) -> com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel? = { _, _ -> null }
    ): DailyActivePlanLoadAction {
        return DailyActivePlanLoadAction(
            getActivePlanId = { activePlanId },
            getActiveFastingType = { FastingPlanType.PLAN_16_8 },
            getCustomFastingHours = { 17 },
            getCustomPlan = { planId ->
                customPlan?.takeIf { it.id == planId }
            },
            createEmptyActivePlan = { emptyPlan },
            createCatalogActivePlan = createCatalogActivePlan,
            loadCustomPlan = {
                assertEquals(customPlan, it)
                customResult
            },
            loadSeedPlan = loadSeedPlan,
            loadCatalogPlan = loadCatalogPlan
        )
    }
}
