package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivePlanApplyActionTest {

    @Test
    fun execute_returnsOpenPlanListWhenNoActivePlanTitleExists() {
        var applyCount = 0
        val action = DailyActivePlanApplyAction { applyCount++ }

        assertTrue(action.execute(null) is DailyActivePlanApplyAction.Result.OpenPlanList)
        assertTrue(action.execute("   ") is DailyActivePlanApplyAction.Result.OpenPlanList)
        assertEquals(0, applyCount)
    }

    @Test
    fun execute_appliesActivePlanWhenTitleExists() {
        var applyCount = 0
        val action = DailyActivePlanApplyAction { applyCount++ }

        val result = action.execute("  Balanced Plan  ")

        val applied = result as DailyActivePlanApplyAction.Result.Applied
        assertEquals("Balanced Plan", applied.planTitle)
        assertEquals(1, applyCount)
    }
}
