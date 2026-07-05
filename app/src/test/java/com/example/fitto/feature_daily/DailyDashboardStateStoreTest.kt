package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_health.HealthConnectDailySummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyDashboardStateStoreTest {

    @Test
    fun `profile water step and nutrition updates are kept in one state`() {
        val store = createStore()
        val updatedTargets = DailyDashboardNutritionTargets(
            calories = 2200,
            proteinGrams = 140,
            carbGrams = 260,
            fatGrams = 75
        )

        store.updateProfileMetrics(
            profileWeightKg = 68.5,
            displayWeightKg = 68.5,
            waterGoalMl = 2750
        )
        store.updateWaterMl(1250)
        store.updateLocalSteps(4321L)
        val change = store.updateSummary(
            nutritionTargets = updatedTargets,
            profileWeightKg = 68.0,
            displayWeightKg = 68.0
        )

        assertTrue(change.didCalorieTargetChange)
        assertEquals(
            DailyDashboardScreenState(
                selectedDate = INITIAL_DATE,
                profileWeightKg = 68.0,
                displayWeightKg = 68.0,
                waterMl = 1250,
                waterGoalMl = 2750,
                localSteps = 4321L,
                nutritionTargets = updatedTargets
            ),
            store.current()
        )
    }


    @Test
    fun `daily calorie target can be refreshed without resetting screen state`() {
        val store = createStore()
        val selectedDate = INITIAL_DATE.plusDays(2)
        store.selectDate(selectedDate)
        store.updateWaterMl(750)

        store.updateDailyCalorieTarget(1950)

        assertEquals(selectedDate, store.current().selectedDate)
        assertEquals(750, store.current().waterMl)
        assertEquals(1950, store.current().nutritionTargets.calories)
    }

    @Test
    fun `same calorie target does not request active plan refresh`() {
        val store = createStore()

        val change = store.updateSummary(
            nutritionTargets = DEFAULT_TARGETS.copy(proteinGrams = 130),
            profileWeightKg = null,
            displayWeightKg = 70.0
        )

        assertFalse(change.didCalorieTargetChange)
    }

    @Test
    fun `health summary is accepted only for selected date`() {
        val store = createStore()
        val summary = HealthConnectDailySummary(
            isAvailable = true,
            hasPermission = true,
            steps = 8200L
        )

        assertFalse(
            store.updateHealthConnectSummary(
                requestedDate = INITIAL_DATE.minusDays(1),
                summary = summary
            )
        )
        assertEquals(HealthConnectDailySummary(), store.current().healthConnectSummary)

        assertTrue(
            store.updateHealthConnectSummary(
                requestedDate = INITIAL_DATE,
                summary = summary
            )
        )
        assertEquals(summary, store.current().healthConnectSummary)
    }

    @Test
    fun `stale active plan result cannot replace latest state`() {
        val store = createStore()
        val staleToken = store.beginActivePlanLoad()
        val latestToken = store.beginActivePlanLoad()
        val stalePlan = activePlan(id = "stale")
        val latestPlan = activePlan(id = "latest")

        assertFalse(
            store.applyActivePlanLoad(
                token = staleToken,
                result = DailyActivePlanLoadResult.Basic(
                    displayPlan = stalePlan,
                    activePlan = stalePlan
                )
            )
        )
        assertNull(store.current().activePlan)

        assertTrue(
            store.applyActivePlanLoad(
                token = latestToken,
                result = DailyActivePlanLoadResult.Basic(
                    displayPlan = latestPlan,
                    activePlan = latestPlan
                )
            )
        )
        assertEquals(latestPlan, store.current().activePlan)
        assertEquals(DEFAULT_TARGETS.calories, latestToken.targetCalories)
    }

    @Test
    fun `selected date is exposed as the current date boundary`() {
        val store = createStore()
        val nextDate = INITIAL_DATE.plusDays(1)

        store.selectDate(nextDate)

        assertTrue(store.isSelectedDate(nextDate))
        assertFalse(store.isSelectedDate(INITIAL_DATE))
        assertEquals(nextDate, store.current().selectedDate)
    }

    private fun createStore(): DailyDashboardStateStore {
        return DailyDashboardStateStore(
            initialDate = INITIAL_DATE,
            initialNutritionTargets = DEFAULT_TARGETS,
            initialDisplayWeightKg = 70.0,
            initialWaterGoalMl = 2500
        )
    }

    private fun activePlan(id: String): ActiveDietPlanUiModel {
        return ActiveDietPlanUiModel(
            id = id,
            title = id,
            badge = "",
            icon = "",
            primaryLabel = "",
            primaryValue = "",
            secondaryLabel = "",
            secondaryValue = "",
            progress = 0,
            kcalPerDay = 1800,
            durationDays = 30
        )
    }

    private companion object {
        val INITIAL_DATE: LocalDate = LocalDate.of(2026, 7, 5)
        val DEFAULT_TARGETS = DailyDashboardNutritionTargets(
            calories = 1800,
            proteinGrams = 120,
            carbGrams = 280,
            fatGrams = 70
        )
    }
}
