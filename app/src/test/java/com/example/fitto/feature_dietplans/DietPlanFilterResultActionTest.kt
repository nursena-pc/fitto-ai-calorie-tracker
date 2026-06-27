package com.example.fitto.feature_dietplans

import com.example.fitto.R
import com.example.fitto.feature_dietplans.DietPlanFilterOptions as FilterOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanFilterResultActionTest {

    @Test
    fun `reset returns clear filter effect with reset toast`() {
        val effect = DietPlanFilterResultAction.reset()

        assertTrue(effect.clearFilters)
        assertNull(effect.state)
        assertEquals(R.string.diet_filter_reset_mock, effect.toastResId)
        assertTrue(effect.closeScreen)
    }

    @Test
    fun `apply returns selected filter state effect with success toast`() {
        val state = DietPlanFilterState(
            goal = FilterOptions.GOAL_MUSCLE_GAIN,
            duration = 30,
            type = FilterOptions.TYPE_KETOGENIC,
            features = setOf(FilterOptions.FEATURE_HIGH_PROTEIN),
            minCalories = 1600,
            maxCalories = 2400
        )

        val effect = DietPlanFilterResultAction.apply(state)

        assertFalse(effect.clearFilters)
        assertEquals(state, effect.state)
        assertEquals(R.string.diet_filter_apply_success, effect.toastResId)
        assertTrue(effect.closeScreen)
    }
}
