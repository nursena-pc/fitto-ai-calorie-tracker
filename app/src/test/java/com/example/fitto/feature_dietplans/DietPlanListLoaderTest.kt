package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanListLoaderTest {

    @Test
    fun load_combinesCustomPlansBeforeReadyPlansAndAppliesState() {
        val activePlan = customPlan("active")
        val draftPlan = customPlan("draft", isDraft = true)
        val readyPlan = readyPlan("ready_saved")
        val customPlanSource = FakeCustomPlanSource(listOf(activePlan, draftPlan))
        val preferenceSource = FakePreferenceSource(
            currentFavoriteIds = setOf(activePlan.id, readyPlan.id),
            currentActivePlanId = activePlan.id
        )
        val readyPlanSource = FakeReadyPlanSource(listOf(readyPlan))

        val snapshot = loader(
            customPlanSource = customPlanSource,
            preferenceSource = preferenceSource,
            readyPlanSource = readyPlanSource
        ).load(includeDrafts = true)

        assertEquals(
            listOf(activePlan.id, draftPlan.id, readyPlan.id),
            snapshot.allPlans.map(DietPlanUiModel::id)
        )
        assertTrue(snapshot.allPlans[0].isFavorite)
        assertTrue(snapshot.allPlans[0].isApplied)
        assertFalse(snapshot.allPlans[1].isFavorite)
        assertFalse(snapshot.allPlans[1].isApplied)
        assertTrue(snapshot.allPlans[2].isFavorite)
        assertFalse(snapshot.allPlans[2].isApplied)
        assertEquals(listOf(true), customPlanSource.includeDraftsCalls)
        assertEquals(preferenceSource.currentFavoriteIds, readyPlanSource.lastFavoriteIds)
        assertEquals(preferenceSource.currentActivePlanId, readyPlanSource.lastActivePlanId)
    }

    @Test
    fun load_excludesDraftsWhenRequested() {
        val activePlan = customPlan("active")
        val draftPlan = customPlan("draft", isDraft = true)

        val snapshot = loader(
            customPlanSource = FakeCustomPlanSource(listOf(activePlan, draftPlan)),
            readyPlanSource = FakeReadyPlanSource(emptyList())
        ).load(includeDrafts = false)

        assertEquals(listOf(activePlan.id), snapshot.customPlans.map(DietPlanUiModel::id))
        assertEquals(listOf(activePlan.id), snapshot.allPlans.map(DietPlanUiModel::id))
    }

    @Test
    fun loadReadyPlans_allowsExplicitFavoriteAndActiveState() {
        val readyPlan = readyPlan("ready")
        val readyPlanSource = FakeReadyPlanSource(listOf(readyPlan))

        val plans = loader(readyPlanSource = readyPlanSource).loadReadyPlans(
            favoriteIds = setOf(readyPlan.id),
            activePlanId = readyPlan.id
        )

        assertEquals(listOf(readyPlan.id), plans.map(DietPlanUiModel::id))
        assertTrue(plans.first().isFavorite)
        assertTrue(plans.first().isApplied)
        assertEquals(setOf(readyPlan.id), readyPlanSource.lastFavoriteIds)
        assertEquals(readyPlan.id, readyPlanSource.lastActivePlanId)
    }

    private fun loader(
        customPlanSource: FakeCustomPlanSource = FakeCustomPlanSource(emptyList()),
        preferenceSource: FakePreferenceSource = FakePreferenceSource(),
        readyPlanSource: FakeReadyPlanSource = FakeReadyPlanSource(emptyList())
    ): DietPlanListLoader {
        return DietPlanListLoader(
            customPlanSource = customPlanSource,
            preferenceSource = preferenceSource,
            readyPlanSource = readyPlanSource
        )
    }

    private fun customPlan(
        suffix: String,
        isDraft: Boolean = false
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$suffix",
            title = "Plan $suffix",
            description = "",
            goalType = "BALANCED",
            dailyCalories = 1800,
            mealCount = 4,
            durationDays = 14,
            isPublic = false,
            preferences = emptyList(),
            exclusions = emptyList(),
            mealOrder = emptyList(),
            isDraft = isDraft,
            createdAt = 0L,
            updatedAt = 0L
        )
    }

    private fun readyPlan(id: String): DietPlanUiModel {
        return DietPlanUiModel(
            id = id,
            titleText = id,
            kcalPerDay = 1700,
            durationDays = 14
        )
    }

    private class FakeCustomPlanSource(
        private val plans: List<CustomDietPlanLocalModel>
    ) : DietPlanListLoader.CustomPlanSource {
        val includeDraftsCalls = mutableListOf<Boolean>()

        override fun getPlans(includeDrafts: Boolean): List<CustomDietPlanLocalModel> {
            includeDraftsCalls += includeDrafts
            return if (includeDrafts) {
                plans
            } else {
                plans.filterNot(CustomDietPlanLocalModel::isDraft)
            }
        }

        override fun toDietPlanUiModel(
            plan: CustomDietPlanLocalModel,
            isFavorite: Boolean
        ): DietPlanUiModel {
            return DietPlanUiModel(
                id = plan.id,
                titleText = plan.title,
                kcalPerDay = plan.dailyCalories,
                durationDays = plan.durationDays,
                isFavorite = isFavorite,
                isDraft = plan.isDraft
            )
        }
    }

    private class FakePreferenceSource(
        val currentFavoriteIds: Set<String> = emptySet(),
        val currentActivePlanId: String? = null
    ) : DietPlanListLoader.PreferenceSource {
        override fun getFavoriteIds(): Set<String> {
            return currentFavoriteIds
        }

        override fun getActivePlanId(): String? {
            return currentActivePlanId
        }
    }

    private class FakeReadyPlanSource(
        private val plans: List<DietPlanUiModel>
    ) : DietPlanListLoader.ReadyPlanSource {
        var lastFavoriteIds: Set<String> = emptySet()
            private set
        var lastActivePlanId: String? = null
            private set

        override fun getReadyPlans(
            favoriteIds: Set<String>,
            activePlanId: String?
        ): List<DietPlanUiModel> {
            lastFavoriteIds = favoriteIds
            lastActivePlanId = activePlanId
            return plans.map { plan ->
                plan.copy(
                    isFavorite = plan.id in favoriteIds,
                    isApplied = plan.id == activePlanId
                )
            }
        }
    }
}
